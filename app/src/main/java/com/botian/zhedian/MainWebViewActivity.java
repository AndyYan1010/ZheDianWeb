package com.botian.zhedian;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.botian.zhedian.activity.CameraPhotoActivity;
import com.botian.zhedian.activity.PDFFileActivity;
import com.botian.zhedian.utils.PhoneInfoUtil;
import com.botian.zhedian.utils.ToastDialogUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.botian.zhedian.utils.ToastDialogUtil.NORMOL_STYLE;

public class MainWebViewActivity extends AppCompatActivity {
    @BindView(R.id.tv_DevID)
    TextView  tv_DevID;
    @BindView(R.id.web_url)
    WebView   web_url;
    @BindView(R.id.img_logo)
    ImageView img_logo;


    private Unbinder unBinder;
    private int      REQUEST_CODE_GET_FACE = 10001;
    private int      RESULT_CODE_FOR_WEB   = 10014;
    private int      webJSType             = -1;
    private int      webType               = -1;
    private String   mCustomerID           = "";
    private String   mDevice               = "";
    private String   mOrderID              = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main_web);
        MyApplication.flag = 0;
        MyApplication.listActivity.add(this);
        initView();
        initData();
        initListener();
    }

    private void initView() {

        unBinder = ButterKnife.bind(this);
    }

    private void initData() {
        //设置webview
        initWebView();
        //获取硬件信息
        getDevInfo();
        Intent intent = new Intent(MainWebViewActivity.this, PDFFileActivity.class);
        intent.putExtra("pdfUrl", "pdfUrl");
        startActivity(intent);
    }

    protected void initListener() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GET_FACE && resultCode == RESULT_CODE_FOR_WEB && null != data) {
            //传给web数据
            transfer2web(data.getStringExtra("resultResbody"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //解除注解
        if (unBinder != null) {
            unBinder.unbind();
        }
        MyApplication.listActivity.remove(this);
    }

    @SuppressLint("JavascriptInterface")
    private void initWebView() {
        //启用支持javascript
        WebSettings settings = web_url.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);//打开本地缓存提供JS调用,至关重要
        settings.setAllowFileAccess(true);
        // 设置允许JS弹窗
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        // 设置可以支持缩放
        //settings.setSupportZoom(true);
        //设置出现缩放工具
        //settings.setBuiltInZoomControls(true);
        //web_url.setInitialScale(70);//100代表不缩放


        web_url.canGoBack();
        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        web_url.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                // 如果url的协议 = 预先约定的 js 协议
                // 就解析往下解析参数
                if (uri.getScheme().equals("js") || uri.getAuthority().equals("")) {
                    // 如果 authority  = 预先约定协议里的 webview，即代表都符合约定的协议
                    // 所以拦截url,下面JS开始调用Android需要的方法
                    if (uri.getAuthority().equals("webview")) {
                        // 执行JS所需要调用的逻辑
                        // 可以在协议上带有参数并传递到Android上

                    }
                    return true;
                }
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }
        });
        //自适应屏幕
        web_url.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        web_url.getSettings().setLoadWithOverviewMode(true);
        web_url.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) {
                    //if (needClear) {
                    //    web_url.clearHistory();
                    //    needClear = false;
                    //}
                }
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                return true;
            }
        });
        /*
         * 为webView 添加一个js 接口
         * 参数一： 是一个java对象
         * 参数二：是一个随意的字符串
         * 该方法的功能是在网页中创建一个js 对象，对象名就是参数二字符串。js对象中的功能，由参数一提供。
         */
        web_url.addJavascriptInterface(new Object() {
            /**
             * 声明一个在js 中可以调用的方法，
             * 注意：4.4以上这里要加注解。
             * 类名：shangyukeji和方法名paizhao跟html保持一致
             */
            @JavascriptInterface
            public void paizhao(int type, String customerID) {
                webJSType   = 0;
                webType     = type;
                mCustomerID = customerID;
                //跳转人脸认证
                step2CheckFace();
            }

            @JavascriptInterface
            public void paizhao1(int type, String device, String orderID) {
                webJSType = 1;
                webType   = type;
                mDevice   = device;
                mOrderID  = orderID;
                //跳转人脸认证
                step2CheckFace();
            }

            @JavascriptInterface
            public void showPDFFile(String pdfUrl) {
                Intent intent = new Intent(MainWebViewActivity.this, PDFFileActivity.class);
                intent.putExtra("pdfUrl", pdfUrl);
                startActivity(intent);
            }
        }, "shangyukeji");
    }

    //传输给web端认证数据
    private void transfer2web(String resultResbody) {
        if (webJSType == 0) {
            web_url.loadUrl("javascript:kaiji('" + resultResbody + "')");
        } else if (webJSType == 1) {
            web_url.loadUrl("javascript:kaiji1('" + resultResbody + "')");
        }
    }

    /***跳转人脸认证*/
    @SuppressLint("CheckResult")
    private void step2CheckFace() {
        new RxPermissions(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .subscribe(granted -> {
                    if (granted) {
                        Intent intent = new Intent(this, CameraPhotoActivity.class);
                        intent.putExtra("ftype", 5);
                        intent.putExtra("webJSType", webJSType);
                        intent.putExtra("webType", webType);
                        intent.putExtra("customerID", mCustomerID);
                        intent.putExtra("device", mDevice);
                        intent.putExtra("orderID", mOrderID);
                        startActivityForResult(intent, REQUEST_CODE_GET_FACE);
                    } else {
                        //未开启定位权限或者被拒绝的操作
                        ToastDialogUtil.getInstance()
                                .setContext(this)
                                .useStyleType(NORMOL_STYLE)
                                .setTitle("无法获取设备读取权限")
                                .setCont("您好，设备需使用相关权限，才能保证软件的正常运行。")
                                .showCancelView(true, "取消", (dialogUtil, view) -> dialogUtil.dismiss())
                                .showSureView(true, "去设置", (dialogUtil, view) -> {
                                    //跳转设置界面
                                    Intent intent = new Intent();
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                                    startActivity(intent);
                                    finish();
                                })
                                .show();
                    }
                });
    }

    @SuppressLint({"CheckResult", "MissingPermission"})
    private void getDevInfo() {
        new RxPermissions(this)
                .request(Manifest.permission.ACCESS_NETWORK_STATE)
                .subscribe(granted -> {
                    if (granted) {
                        MyApplication.devID = PhoneInfoUtil.getTelMacAddress().replace(":", "");
                        //MyApplication.devID = "869066035238777";
                        tv_DevID.setText("设备ID：" + MyApplication.devID);
                        //webview加载网页地址.
                        //web_url.loadUrl("http://81.68.102.112:8088/h5?devID=" + MyApplication.devID);
                        web_url.loadUrl("http://10.0.0.11:42007/haianWeb?devID=" + MyApplication.devID);
                    } else {
                        //未开启定位权限或者被拒绝的操作
                        ToastDialogUtil.getInstance()
                                .setContext(this)
                                .useStyleType(NORMOL_STYLE)
                                .setTitle("无法获取设备读取权限")
                                .setCont("您好，设备需使用相关权限，才能保证软件的正常运行。")
                                .showCancelView(true, "取消", (dialogUtil, view) -> dialogUtil.dismiss())
                                .showSureView(true, "去设置", (dialogUtil, view) -> {
                                    //跳转设置界面
                                    Intent intent = new Intent();
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                                    startActivity(intent);
                                    finish();
                                })
                                .show();
                    }
                });
    }
}
