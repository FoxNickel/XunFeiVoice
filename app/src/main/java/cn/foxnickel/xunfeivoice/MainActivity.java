package cn.foxnickel.xunfeivoice;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.iflytek.cloud.EvaluatorListener;
import com.iflytek.cloud.EvaluatorResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvaluator;
import com.iflytek.cloud.SpeechUtility;

import cn.foxnickel.xunfeivoice.result.Result;

public class MainActivity extends BaseActivity implements View.OnClickListener{

    private EditText mEvaluateText;//要读的字符串
    private EditText mParseResult;//解析出来的结果
    private Button mStartRead,mParse,mStopRead,mChineseEvaluate;//开始读，解析

    // 评测语种
    private String language = "en_us";
    // 评测题型
    private String category;
    // 结果等级
    private String result_level;

    private String mLastResult;//解析结果
    private SpeechEvaluator mIse;//评估组件

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=58ca19e8");

        mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
        mIse = SpeechEvaluator.createEvaluator(this,null);//初始化
        initUI();
    }

    private void initUI() {
        mEvaluateText = (EditText) findViewById(R.id.text_evaluate);
        mParseResult = (EditText) findViewById(R.id.parse_result);
        mStartRead = (Button) findViewById(R.id.begin_evaluate);
        mStopRead = (Button) findViewById(R.id.stop_evaluate);
        mParse = (Button) findViewById(R.id.parse);
        mChineseEvaluate = (Button) findViewById(R.id.chinese_evaluate);
        mEvaluateText.setOnClickListener(this);
        mParseResult.setOnClickListener(this);
        mParse.setOnClickListener(this);
        mStartRead.setOnClickListener(this);
        mStopRead.setOnClickListener(this);
        mChineseEvaluate.setOnClickListener(this);
    }

    private void setParams() {
        /* 设置评测语言
        language = "en_us";*/
        // 设置需要评测的类型
        category = "read_sentence";
        // 设置结果等级（中文仅支持complete）
        result_level = "complete";
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        String vad_bos = "5000";
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        String vad_eos = "1800";
        // 语音输入超时时间，即用户最多可以连续说多长时间；
        String speech_timeout = "-1";

        mIse.setParameter(SpeechConstant.LANGUAGE, language);
        mIse.setParameter(SpeechConstant.ISE_CATEGORY, category);
        mIse.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        mIse.setParameter(SpeechConstant.VAD_BOS, vad_bos);
        mIse.setParameter(SpeechConstant.VAD_EOS, vad_eos);
        mIse.setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT, speech_timeout);
        mIse.setParameter(SpeechConstant.RESULT_LEVEL, result_level);

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        /*mIse.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIse.setParameter(SpeechConstant.ISE_AUDIO_PATH, Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/ise.wav");*/
    }

    @Override
    public void onClick(View v) {
        if( null == mIse ){
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip( "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化" );
            return;
        }

        int id = v.getId();
        switch (id){
            case R.id.begin_evaluate:
                if (mIse == null) {
                    return;
                }
                setParams();
                String evaluateText = mEvaluateText.getText().toString();
                mIse.startEvaluating(evaluateText,null,mEvaluatorListener);
                break;
            case R.id.parse:
                if (!TextUtils.isEmpty(mLastResult)) {
                    XmlResultParser resultParser = new XmlResultParser();
                    Result result = resultParser.parse(mLastResult);

                    if (null != result) {
                        mParseResult.setText(result.toString());
                    } else {
                        showTip("解析结果为空");
                    }
                }
                break;
            case R.id.stop_evaluate:
                if (mIse.isEvaluating()) {
                    mParseResult.setHint("评测已停止，等待结果中...");
                    mIse.stopEvaluating();
                }
                break;
            case R.id.chinese_evaluate:
                language = "zh_cn";
                showTip("切换为中文模式");
                break;
        }
    }


    private void showTip(String str) {
        if(!TextUtils.isEmpty(str)) {
            mToast.setText(str);
            mToast.show();
        }
    }

    // 评测监听接口
    private EvaluatorListener mEvaluatorListener = new EvaluatorListener() {

        @Override
        public void onResult(EvaluatorResult result, boolean isLast) {

            if (isLast) {
                StringBuilder builder = new StringBuilder();
                builder.append(result.getResultString());

                if(!TextUtils.isEmpty(builder)) {
                    mParseResult.setText(builder.toString());
                }
                mLastResult = builder.toString();

                showTip("评测结束");
            }
        }

        @Override
        public void onError(SpeechError error) {
            if(error != null) {
                showTip("error:"+ error.getErrorCode() + "," + error.getErrorDescription());
            } else {

            }
        }

        @Override
        public void onBeginOfSpeech() {
            showTip("请开始讲话");
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前音量：" + volume);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }

    };
}
