package afas.software.ssu.com.afas;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import afas.software.ssu.com.afas.utils.AudioWriterPCM;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static afas.software.ssu.com.afas.R.id.getAuthorization;
import static afas.software.ssu.com.afas.R.id.text;
import static android.media.AudioManager.ERROR;


public class MainActivity extends AppCompatActivity {
    private TextToSpeech myTTS;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CLIENT_ID = "toBgPrtQQ2e8rDKWLVGG";
    // 1. "내 애플리케이션"에서 Client ID를 확인해서 이곳에 적어주세요.
    // 2. build.gradle (Module:app)에서 패키지명을 실제 개발자센터 애플리케이션 설정의 '안드로이드 앱 패키지 이름'으로 바꿔 주세요

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    private TextView txtResult;
    private Button btnStart;
    private String mResult;

    private Button btn;

    private AudioWriterPCM writer;

    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                txtResult.setText("Connected");
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                txtResult.setText(mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                mResult = results.get(0);
//                StringBuilder strBuf = new StringBuilder();
//                for(String result : results) {
//                    strBuf.append(results);
//                    strBuf.append("\n");
//                }

//                mResult = strBuf.toString();

                if(mResult.contains("날씨"))
                    mResult = "";
//                    txtResult.setText("날씨에관련된설명");


                if(mResult.contains("노래"))
                    mResult = "현재 시각 12월 15일 3시 기준 노래 1위는 트와이스의 하트쉐이커입니다";

                if(mResult.contains("고마워"))
                    mResult = "별 말씀을요";

                myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != ERROR) {
                            // 언어를 선택한다.
                            myTTS.setLanguage(Locale.KOREAN);
                            myTTS.speak(mResult,TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                });


                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                txtResult.setText(mResult);
                btnStart.setText(R.string.str_start);
                btnStart.setEnabled(true);
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }

                btnStart.setText(R.string.str_start);
                btnStart.setEnabled(true);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResult = (TextView) findViewById(R.id.txt_result);
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(mClickListener);

        btn = (Button)findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                    String result = "안녕하세요 혜민님 오늘은 12월 15일 날씨는 흐림입니다. 현재 기온은 영상 1도 강수확률은 60% 입니다.";
                    @Override
                    public void onInit(int status) {
                        if(status != ERROR) {
                            // 언어를 선택한다.
                            myTTS.setLanguage(Locale.KOREAN);
                            myTTS.speak(result,TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                });

            }
        });

        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);

        btnStart.performClick();


    }

    @Override
    protected void onStart() {
        super.onStart();
        // NOTE : initialize() must be called on start time.
        naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mResult = "";
        txtResult.setText("");
        btnStart.setText(R.string.str_start);
        btnStart.setEnabled(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.
        naverRecognizer.getSpeechRecognizer().release();
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        RecognitionHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    Button.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(),"강제로눌림",Toast.LENGTH_SHORT).show();
            if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                // Start button is pushed when SpeechRecognizer's state is inactive.
                // Run SpeechRecongizer by calling recognize().
                Toast.makeText(getApplicationContext(),"강제로눌림1",Toast.LENGTH_SHORT).show();
                mResult = "";
                txtResult.setText("Connecting...");
                btnStart.setText(R.string.str_stop);
                naverRecognizer.recognize();

            } else {
                Log.d(TAG, "stop and wait Final Result");
                btnStart.setEnabled(false);

                naverRecognizer.getSpeechRecognizer().stop();
            }

        }
    };


//
//        @Override
//        public void onClick(View v) {
//            if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
//                // Start button is pushed when SpeechRecognizer's state is inactive.
//                // Run SpeechRecongizer by calling recognize().
//                mResult = "";
//                txtResult.setText("Connecting...");
//                btnStart.setText(R.string.str_stop);
//                naverRecognizer.recognize();
//            } else {
//                Log.d(TAG, "stop and wait Final Result");
//                btnStart.setEnabled(false);
//
//                naverRecognizer.getSpeechRecognizer().stop();
//            }
//        }
//    });


}
