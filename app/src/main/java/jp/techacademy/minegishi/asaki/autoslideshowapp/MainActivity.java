package jp.techacademy.minegishi.asaki.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    Timer mTimer;
    TextView mPageText;      // スライドのページ数を表示

    int mPage = 1;           // 現在のページ数のための変数
    int mAllPage = 0;        // 総ページ数のための変数
    int ButtonCheck = 0;     //　mStartStopButtonの状態管理変数。0：停止　1：再生

    Handler mHandler = new Handler();

    Button mNextButton;  // 進むボタン
    Button mPreviousButton;  // 戻るボタン
    Button mStartStopButton;  // 再生停止ボタン

    ContentResolver resolver;  // ContentResolverクラスから生成されたインスタンスresolverによるメンバ変数
    Cursor cursor;  // Cursorクラスから生成されたインスタンスcursorによるメンバ変数


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPageText = (TextView) findViewById(R.id.timer);
        mNextButton = (Button) findViewById(R.id.start_button);
        mPreviousButton = (Button) findViewById(R.id.pause_button);
        mStartStopButton = (Button) findViewById(R.id.reset_button);

        // パーミッション
        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo();
        }


        // 進むボタンが押された時
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cursor.moveToNext()) {  // 次に移動したカーソルがtrueの場合
                    ImageVIew();
                    mPage++;
                }else {                     // 次に移動したカーソルがfalseの場合
                    cursor.moveToFirst();
                    ImageVIew();
                    mPage = 1;
                }
                mPageText.setText(String.format("%d/%d", mPage, mAllPage));
            }
        });

        // 戻るボタンが押された時
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cursor.moveToPrevious()) {  // 前に移動したカーソルがtrueの場合
                    ImageVIew();
                    mPage--;
                } else {                        // 前に移動したカーソルがfalseの場合
                    cursor.moveToLast();
                    ImageVIew();
                    mPage = mAllPage;
                }
                mPageText.setText(String.format("%d/%d", mPage, mAllPage));
            }
        });

        // 再生停止ボタンが押された時
        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ButtonCheck == 0) {  // ボタンが停止状態の時
                    mNextButton.setEnabled(false);
                    mPreviousButton.setEnabled(false);
                    mStartStopButton.setText("停止");
                    ButtonCheck = 1;

                    if (mTimer == null) {
                        // タイマーの作成
                        mTimer = new Timer();
                        // タイマーの始動
                        mTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {            // run() 関数が指定時間毎に呼び出される
                                if (mPage != mAllPage) {   // 現在のページを確認
                                    mPage += 1;
                                    cursor.moveToNext();
                                } else {
                                    mPage = 1;
                                    cursor.moveToFirst();
                                }

                                mHandler.post(new Runnable() {  // 描画の依頼 Handler はスレッドを超えて依頼をするために使用
                                    @Override
                                    public void run() {         // run() 内の処理は UI 描画なので、メインスレッドに依頼

                                        ImageVIew();
                                        mPageText.setText(String.format("%d/%d", mPage, mAllPage));

                                    }
                                });
                            }
                        }, 2000, 2000);    // 最初に始動させるまで 2000ミリ秒、ループの間隔を 2000ミリ秒 に設定
                    }
                }else {    // ボタンが再生状態の時
                    mNextButton.setEnabled(true);
                    mPreviousButton.setEnabled(true);
                    mStartStopButton.setText("再生");
                    ButtonCheck = 0;
                    if (mTimer != null) {  // タイマーリセット
                        mTimer.cancel();
                        mTimer = null;
                    }
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo();
                }else {
                    // 許可されなかった場合は、全ボタンを無効にし、許可されなかったことを表示
                    mNextButton.setEnabled(false);
                    mPreviousButton.setEnabled(false);
                    mStartStopButton.setEnabled(false);
                    mPageText.setTextSize(30);
                    mPageText.setText(String.format("許可されませんでした"));
                }
                break;
            default:
                break;
        }
    }

    // 画像の情報を取得し、最初の画像を描画
    private void getContentsInfo() {

        // 画像の情報を取得する
        resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );

            mAllPage = cursor.getCount();
            mPageText.setText(String.format("%d/%d", mPage, mAllPage));


        if (cursor.moveToFirst()) {
            ImageVIew();
        }else {
            cursor.close();
            mPage = 0;
            mNextButton.setEnabled(false);
            mPreviousButton.setEnabled(false);
            mStartStopButton.setEnabled(false);
        }

    }

    // indexからIDを取得し、そのIDから画像のURIを取得し描画
    private void ImageVIew() {
        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

        ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
        imageVIew.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageVIew.setImageURI(imageUri);
    }

}
