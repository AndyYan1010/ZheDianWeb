package com.botian.zhedian.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botian.zhedian.BaseActivity;
import com.botian.zhedian.MyApplication;
import com.botian.zhedian.NetConfig;
import com.botian.zhedian.R;
import com.botian.zhedian.adapter.PersonListAdapter;
import com.botian.zhedian.bean.FaceResultBean;
import com.botian.zhedian.bean.UpResultBean;
import com.botian.zhedian.configure.LocalSetting;
import com.botian.zhedian.utils.ToastDialogUtil;
import com.botian.zhedian.utils.ToastUtils;
import com.botian.zhedian.utils.fileUtils.AudioUtil;
import com.botian.zhedian.utils.fileUtils.FileUtil;
import com.botian.zhedian.utils.mediaUtils.CameraAutoUtil;
import com.botian.zhedian.utils.mediaUtils.SoundUtil;
import com.botian.zhedian.utils.netUtils.OkHttpUtils;
import com.botian.zhedian.utils.netUtils.RequestParamsFM;
import com.google.gson.Gson;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import okhttp3.Request;

import static com.botian.zhedian.utils.ToastDialogUtil.NORMOL_STYLE;

public class CameraPhotoActivity extends BaseActivity implements View.OnClickListener {
    @BindView(R.id.tv_time)
    TextView tv_time;
    @BindView(R.id.tv_title)
    TextView tv_title;
    //@BindView(R.id.sfview)
    SurfaceView sfview;
    @BindView(R.id.img_fontBorder)
    ImageView    img_fontBorder;
    @BindView(R.id.img_back)
    ImageView    img_back;
    @BindView(R.id.preview_parent)
    LinearLayout preview_parent;
    @BindView(R.id.rec_person)
    RecyclerView rec_person;
    @BindView(R.id.card_view)
    CardView     card_view;
    @BindView(R.id.tv_sure)
    TextView     tv_sure;

    private List<UpResultBean.ListBean> mRecPersonList;
    private PersonListAdapter           personListAdapter;
    private String[]                    mListPermission           = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private int                         RESULT_CODE_OPEN_MACHINE  = 10012;
    private int                         RESULT_CODE_CLOSE_MACHINE = 10013;
    private int                         RESULT_CODE_FOR_WEB       = 10014;
    private String                      faceCheckResbody;

    @Override
    public int setLayout() {
        return R.layout.act_camera_photo;
    }

    @Override
    public void initView(Bundle savedInstanceState) {
        sfview = new SurfaceView(this);
        LinearLayout.LayoutParams lp = new
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        sfview.setLayoutParams(lp);
    }

    @Override
    public void initData() {
        //?????????????????????
        initRecPerson();
    }

    @Override
    public void initListener() {
        img_back.setOnClickListener(this);
        tv_sure.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.tv_sure:
                //???????????????act
                card_view.setVisibility(View.GONE);
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        preview_parent.addView(sfview);
        showCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        preview_parent.removeView(sfview);
        CameraAutoUtil.getInstance().stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    /**
     * ????????????
     */
    @SuppressLint("CheckResult")
    private void showCamera() {
        new RxPermissions(this)
                .request(mListPermission)
                .subscribe(granted -> {
                    if (granted) {
                        //?????????????????????????????????SurfaceView???
                        setSurFaceView();
                    } else {
                        //?????????????????????????????????????????????
                        ToastDialogUtil.getInstance()
                                .setContext(this)
                                .useStyleType(NORMOL_STYLE)
                                .setTitle("????????????????????????")
                                .setCont("???????????????????????????????????????????????????????????????????????????")
                                .showCancelView(true, "??????", (dialogUtil, view) -> dialogUtil.dismiss())
                                .showSureView(true, "?????????", (dialogUtil, view) -> {
                                    //??????????????????
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

    /**
     * ????????????
     */
    private void setSurFaceView() {
        CameraAutoUtil.getInstance()
                .initView(this, sfview, img_fontBorder)
                .setFtype(getFtype())
                .setWebJSType(getWebJSType())
                .setWebType(getWebType())
                .setCustomerID(getCustomertID())
                .setDeviceID(getDeviceID())
                .setOrderID(getOrderID())
                .setContChangeListener((titleCont, cont) -> {
                    setCont(tv_title, titleCont);
                    setCont(tv_time, cont);
                })
                .setFinishListener(new CameraAutoUtil.FinishListener() {
                    @Override
                    public void onWaiteStart() {
                        CameraAutoUtil.getInstance().countGetFace();
                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onResult(int ftype, String resbody, List<FaceResultBean.DataBean> personList) {
                        //?????????????????????????????????
                        if (ftype == 0) {
                            //??????????????????
                            go2Work(personList);
                        } else if (ftype == 1) {
                            //??????????????????
                            offWork(personList);
                        } else if (ftype == 2) {
                            //??????????????????????????????
                            openMachineOrAddPerson(personList);
                        } else if (ftype == 3) {
                            //??????
                            closeMachine(personList);
                        } else if (ftype == 4) {
                            //??????

                        } else if (ftype == 5) {
                            //??????web??????
                            faceCheckResbody = resbody;
                            Intent intent = new Intent();
                            intent.putExtra("ftype", getFtype());
                            intent.putExtra("resultResbody", faceCheckResbody);
                            setResult(RESULT_CODE_FOR_WEB, intent);
                            finish();
                        } else {
                            ToastUtils.showToast("?????????????????????????????????");
                        }
                    }

                    @Override
                    public void onFinish(String result) {
                        if (2 == getFtype()) {
                            Intent intent = new Intent();
                            intent.putExtra("ftype", getFtype());
                            intent.putExtra("addType", getIntent().getIntExtra("addType", -1));
                            intent.putExtra("itemIndex", getIntent().getIntExtra("itemIndex", -1));
                            setResult(RESULT_CODE_OPEN_MACHINE, intent);
                        }
                        if (3 == getFtype()) {
                            Intent intent = new Intent();
                            intent.putExtra("ftype", getFtype());
                            intent.putExtra("itemIndex", getIntent().getIntExtra("itemIndex", -1));
                            setResult(RESULT_CODE_CLOSE_MACHINE, intent);
                        }
                        finish();
                    }
                })
                .setSurFaceView();
    }

    public int getFtype() {
        return getIntent().getIntExtra("ftype", -1);
    }

    public int getWebJSType() {
        return getIntent().getIntExtra("webJSType", -1);
    }

    public int getWebType() {
        return getIntent().getIntExtra("webType", -1);
    }

    public String getCustomertID() {
        return getIntent().getStringExtra("customerID");
    }

    public String getDeviceID() {
        return getIntent().getStringExtra("device");
    }

    public String getOrderID() {
        return getIntent().getStringExtra("orderID");
    }

    /****??????????????????*/
    public void showPersonList(List<UpResultBean.ListBean> listBeans) {
        if (null == listBeans || listBeans.size() == 0) {
            ToastUtils.showToast("???????????????????????????");
            return;
        }
        mRecPersonList.clear();
        mRecPersonList.addAll(listBeans);
        card_view.setVisibility(View.VISIBLE);
        personListAdapter.notifyDataSetChanged();
    }

    /***???????????????????????????*/
    private void initRecPerson() {
        mRecPersonList = new ArrayList<>();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rec_person.setLayoutManager(linearLayoutManager);
        personListAdapter = new PersonListAdapter(this, R.layout.recy_person_list, mRecPersonList);
        rec_person.setAdapter(personListAdapter);
    }

    private void setCont(TextView textView, String cont) {
        if (null != textView)
            textView.setText(cont);
    }

    /****??????*/
    private void closeMachine(List<FaceResultBean.DataBean> personList) {
        CameraAutoUtil.getInstance().callContChangeListener("", "????????????????????????...");
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < personList.size(); i++) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", personList.get(i).getPersonName());
                jsonObject.put("userid", personList.get(i).getPersonId());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
        }
        RequestParamsFM params = new RequestParamsFM();
        params.put("workid", MyApplication.workID);
        params.put("starttime", MyApplication.workStartTime);
        params.put("peoplelist", jsonArray);
        params.setUseJsonStreamer(true);
        OkHttpUtils.getInstance().doPost(NetConfig.HBGUANJI, params, new OkHttpUtils.HttpCallBack() {
            @Override
            public void onError(Request request, IOException e) {
                ToastUtils.showToast("????????????");
                SoundUtil.getInstance().playSrcAudio(R.raw.repeatclock);
                CameraAutoUtil.getInstance().callContChangeListener("", "????????????");
            }

            @Override
            public void onSuccess(int code, String resbody) {
                if (code != 200) {
                    CameraAutoUtil.getInstance().callContChangeListener("", "?????????????????????");
                    ToastUtils.showToast("?????????????????????");
                    SoundUtil.getInstance().playSrcAudio(R.raw.repeatclock);
                    return;
                }
                CameraAutoUtil.getInstance().callContChangeListener("", "");
                Gson         gson         = new Gson();
                UpResultBean upResultBean = gson.fromJson(resbody, UpResultBean.class);
                ToastUtils.showToast(upResultBean.getMessage());
                //if (!"1".equals(upResultBean.getCode())) {
                //    SoundUtil.getInstance().playSrcAudio(R.raw.repeatclock);
                //    return;
                //}
                //??????????????????
                showPersonList(upResultBean.getList());
                //????????????
                playAudio(upResultBean.getAudio());
            }
        });
    }

    /****?????????????????????*/
    private void openMachineOrAddPerson(List<FaceResultBean.DataBean> personList) {
        if (getIntent().getIntExtra("addType", -1) == -1) {
            ToastUtils.showToast("???????????????????????????????????????");
            return;
        }
        CameraAutoUtil.getInstance().callContChangeListener("", "????????????????????????...");
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < personList.size(); i++) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", personList.get(i).getPersonName());
                jsonObject.put("userid", personList.get(i).getPersonId());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
        }
        RequestParamsFM params = new RequestParamsFM();
        params.put("type", getIntent().getIntExtra("addType", -1));
        params.put("workid", MyApplication.workID);
        params.put("peoplelist", jsonArray);
        params.setUseJsonStreamer(true);
        OkHttpUtils.getInstance().doPost(NetConfig.ADDRY2, params, new OkHttpUtils.HttpCallBack() {
            @Override
            public void onError(Request request, IOException e) {
                ToastUtils.showToast("????????????");
                SoundUtil.getInstance().playSrcAudio(R.raw.repeatclock);
                CameraAutoUtil.getInstance().callContChangeListener("", "????????????");
            }

            @Override
            public void onSuccess(int code, String resbody) {
                if (code != 200) {
                    ToastUtils.showToast("?????????????????????");
                    SoundUtil.getInstance().playSrcAudio(R.raw.repeatclock);
                    CameraAutoUtil.getInstance().callContChangeListener("", "?????????????????????");
                    return;
                }
                CameraAutoUtil.getInstance().callContChangeListener("", "");
                Gson         gson         = new Gson();
                UpResultBean upResultBean = gson.fromJson(resbody, UpResultBean.class);
                ToastUtils.showToast(upResultBean.getMessage());
                //if (!"1".equals(upResultBean.getCode())) {
                //    SoundUtil.getInstance().playSrcAudio(R.raw.repeatclock);
                //    return;
                //}
                //??????????????????
                showPersonList(upResultBean.getList());
                //????????????
                playAudio(upResultBean.getAudio());
            }
        });
    }

    //??????
    private void offWork(List<FaceResultBean.DataBean> personList) {
        CameraAutoUtil.getInstance().callContChangeListener("", "????????????????????????...");
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < personList.size(); i++) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", personList.get(i).getPersonName());
                jsonObject.put("userid", personList.get(i).getPersonId());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
        }
        RequestParamsFM params = new RequestParamsFM();
        params.put("peoplelist", jsonArray);
        params.setUseJsonStreamer(true);
        OkHttpUtils.getInstance().doPost(NetConfig.ENDWORK, params, new OkHttpUtils.HttpCallBack() {
            @Override
            public void onError(Request request, IOException e) {
                ToastUtils.showToast("????????????");
                SoundUtil.getInstance().playSrcAudio(R.raw.nogowork);
                CameraAutoUtil.getInstance().callContChangeListener("", "????????????");
            }

            @Override
            public void onSuccess(int code, String resbody) {
                if (code != 200) {
                    ToastUtils.showToast("?????????????????????");
                    SoundUtil.getInstance().playSrcAudio(R.raw.nogowork);
                    CameraAutoUtil.getInstance().callContChangeListener("", "?????????????????????");
                    return;
                }
                CameraAutoUtil.getInstance().callContChangeListener("", "");
                Gson         gson         = new Gson();
                UpResultBean upResultBean = gson.fromJson(resbody, UpResultBean.class);
                ToastUtils.showToast(upResultBean.getMessage());
                //if ("0".equals(upResultBean.getCode())) {
                //    SoundUtil.getInstance().playSrcAudio(R.raw.nogowork);
                //    return;
                //}
                //if ("1".equals(upResultBean.getCode())) {
                //    SoundUtil.getInstance().playSrcAudio(R.raw.noclose);
                //    return;
                //}
                //??????????????????
                showPersonList(upResultBean.getList());
                //????????????
                playAudio(upResultBean.getAudio());
            }
        });
    }

    //??????
    private void go2Work(List<FaceResultBean.DataBean> personList) {
        CameraAutoUtil.getInstance().callContChangeListener("", "????????????????????????...");
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < personList.size(); i++) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("username", personList.get(i).getPersonName());
                jsonObject.put("userid", personList.get(i).getPersonId());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(jsonObject);
        }
        RequestParamsFM params = new RequestParamsFM();
        params.put("peoplelist", jsonArray);
        params.setUseJsonStreamer(true);
        OkHttpUtils.getInstance().doPost(NetConfig.STARTWORK, params, new OkHttpUtils.HttpCallBack() {
            @Override
            public void onError(Request request, IOException e) {
                ToastUtils.showToast("????????????");
                SoundUtil.getInstance().playSrcAudio(R.raw.noafterwork);
                CameraAutoUtil.getInstance().callContChangeListener("", "????????????");
            }

            @Override
            public void onSuccess(int code, String resbody) {
                if (code != 200) {
                    ToastUtils.showToast("?????????????????????");
                    SoundUtil.getInstance().playSrcAudio(R.raw.noafterwork);
                    CameraAutoUtil.getInstance().callContChangeListener("", "?????????????????????");
                    return;
                }
                Gson         gson         = new Gson();
                UpResultBean upResultBean = gson.fromJson(resbody, UpResultBean.class);
                ToastUtils.showToast(upResultBean.getMessage());
                CameraAutoUtil.getInstance().callContChangeListener("", "");
                //if (!"1".equals(upResultBean.getCode())) {
                //    SoundUtil.getInstance().playSrcAudio(R.raw.noafterwork);
                //    //return;
                //}
                //??????????????????
                showPersonList(upResultBean.getList());
                //????????????
                playAudio(upResultBean.getAudio());
            }
        });
    }

    private String audioFilePath = "";

    /***????????????
     * @param audio*/
    private void playAudio(String audio) {
        audioFilePath = LocalSetting.AUDIO_PATH + "bobao.wav";
        FileUtil.deleteFile(audioFilePath);
        AudioUtil.getInstance().setChangeListener(new AudioUtil.ChangeFileListener() {
            @Override
            public void onSuccess() {
                //????????????
                SoundUtil.getInstance().playAudio(audioFilePath);
            }

            @Override
            public void onFailed() {
                ToastUtils.showToast("?????????????????????");
            }
        }).decoderBase642File(audio, audioFilePath);
    }
}
