package com.buynme.buynme.extras;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.buynme.buynme.Interface.DownloadListener;
import com.buynme.buynme.configurations.ApplicationDatas;
import com.buynme.buynme.utilities.DebugLog;
import com.buynme.buynme.utilities.FileUtils;
import com.buynme.buynme.utilities.OkHttpUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by phamdinhan on 01/09/2016.
 */
public class Downloader {
    public static final String DOWNLOAD_TAG = "Downloader_Tag";

    private static final int DOWNLOAD_RESULT_COMPLETE = 200;
    private static final int DOWNLOAD_RESULT_FAIL = 201;
    private static final int DOWNLOAD_RESULT_CANCEL_BY_USER = 202;
    private static final int DOWNLOAD_RESULT_CODE_WRONG_URL = 404;
    private static Downloader INSTANCE = null;

    public static Downloader getInstance() {
        if (INSTANCE == null) {
            synchronized (Downloader.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Downloader();
                }
            }
        }
        return INSTANCE;
    }

    private Context mContext;
    private DownloadListener mDownloadListener;
    private String URL;
    private ProgressBar mProgressBar;
    private TextView mTextViewPercent, mTextViewComplete;
    private boolean isDownloading;
    private boolean isForceCancelDownload;

    private Downloader() {
        mContext = ApplicationDatas.getInstance().getApplicationContext();
        isDownloading = false;
        isForceCancelDownload = false;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void cancelDownload() {
        isForceCancelDownload = true;
    }

    /**
     * @param fileUrl          arrayList Url download
     * @param progressBar      progressbar to update download process
     * @param textViewPercent  update percent download of a file
     * @param textViewComplete update complete number file downloaded on listUrls
     */
    public void addDownloadFile(String directory, String fileUrl, ProgressBar progressBar,
                                TextView textViewPercent, TextView textViewComplete, DownloadListener listener) {
        if (isDownloading == true) {
            return;
        }
        this.URL = fileUrl;
        this.mProgressBar = progressBar;
        this.mTextViewPercent = textViewPercent;
        this.mTextViewComplete = textViewComplete;
        this.mDownloadListener = listener;
        run(directory);
    }

    private void run(final String directory) {
        isDownloading = true;

        new AsyncTask<Void, Integer, Integer>() {
            String desFilePath;
            int currentIndex;

            @Override
            protected Integer doInBackground(Void... voids) {
                OkHttpClient client = new OkHttpClient();

                // temp
                File tempDir = mContext.getDir(directory, Context.MODE_PRIVATE);
                File tempFile = new File(tempDir, "temp_download.dat");
                // The destination file will be save
                File desFile;
                // buffer
                final byte[] fileReader = new byte[4096];

                currentIndex = 1;
                publishProgress(0);

                if (tempFile.exists() == true) {
                    tempFile.delete();
                }

                String fileUrl = URL;
                desFilePath = FileUtils.getFilePathToSaveFile(mContext, fileUrl, directory);
                desFile = new File(desFilePath);

                if (TextUtils.isEmpty(fileUrl) == true) {
                    return DOWNLOAD_RESULT_CODE_WRONG_URL;
                } else if (desFile.exists() == true) {
                    return DOWNLOAD_RESULT_COMPLETE;
                } else {
                    InputStream inputStream = null;
                    BufferedOutputStream outputStream = null;

                    try {
                        String csrftoken = String.valueOf(System.currentTimeMillis());
                        Request request = new Request.Builder().url(fileUrl)
                                .addHeader("X-CSRFToken", csrftoken)
                                .tag(DOWNLOAD_TAG)
                                .build();

                        Response response = client.newCall(request).execute();
                        long fileSize = response.body().contentLength();
                        DebugLog.d("fileSize", fileSize + "");
                        long fileSizeDownloaded = 0;

                        inputStream = response.body().byteStream();
                        outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
                        while (isForceCancelDownload == false) {

                            int read = inputStream.read(fileReader);
                            if (read == -1) {
                                break;
                            }

                            outputStream.write(fileReader, 0, read);
                            fileSizeDownloaded += read;
                            publishProgress((int) (fileSizeDownloaded * 100 / fileSize));
                            DebugLog.d("fileSizeDownloaded", fileSizeDownloaded + "");
                        }

                        if (isForceCancelDownload == false) {
                            outputStream.flush();

                            //------------------------------------------------------------------
                            if (tempFile.exists() == true) {
                                tempFile.renameTo(desFile);
                            }
                        } else {
                            // cancel http request
                            OkHttpUtils.cancelCallWithTag(client, DOWNLOAD_TAG);
                        }

                    } catch (Exception ex) {
                        return DOWNLOAD_RESULT_FAIL;
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if (isForceCancelDownload == true) {
                    isForceCancelDownload = false;
                    return DOWNLOAD_RESULT_CANCEL_BY_USER;
                }
                return DOWNLOAD_RESULT_COMPLETE;
            }

            @Override
            public void onProgressUpdate(Integer... args) {
                int percent = args[0];

                if (mProgressBar != null) {
                    mProgressBar.setProgress(percent);
                }
                if (mTextViewPercent != null) {
                    mTextViewPercent.setText(percent + "%");
                }
                if (mTextViewComplete != null) {
                    mTextViewComplete.setText("1/1");
                }
            }

            @Override
            protected void onPostExecute(Integer resultCODE) {
                isDownloading = false;
                URL = "";

                switch (resultCODE) {
                    case DOWNLOAD_RESULT_COMPLETE:
                        if (mDownloadListener != null) {
                            mDownloadListener.onDownloadComplete(desFilePath);
                        }
                        break;

                    case DOWNLOAD_RESULT_FAIL:
                        if (mDownloadListener != null) {
                            mDownloadListener.onDownloadFail("Download error");
                        }
                        break;

                    case DOWNLOAD_RESULT_CANCEL_BY_USER:
                        if (mDownloadListener != null) {
                            mDownloadListener.onDownloadCanceled();
                        }
                        break;

                    case DOWNLOAD_RESULT_CODE_WRONG_URL:
                        if (mDownloadListener != null) {
                            mDownloadListener.onDownloadFail("File not found");
                        }
                        break;

                }
            }

        }.execute();
    }

}
