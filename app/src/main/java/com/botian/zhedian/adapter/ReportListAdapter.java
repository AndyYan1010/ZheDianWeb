package com.botian.zhedian.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botian.zhedian.MyApplication;
import com.botian.zhedian.NetConfig;
import com.botian.zhedian.R;
import com.botian.zhedian.activity.CameraPhotoActivity;
import com.botian.zhedian.bean.ReportListInfo;
import com.botian.zhedian.bean.ReportPersonListBean;
import com.botian.zhedian.utils.PopupOpenHelper;
import com.botian.zhedian.utils.TimeUtil;
import com.botian.zhedian.utils.ToastUtils;
import com.botian.zhedian.utils.netUtils.OkHttpUtils;
import com.botian.zhedian.utils.netUtils.RequestParamsFM;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

public class ReportListAdapter extends BaseQuickAdapter<ReportListInfo.ListBean, BaseViewHolder> {
    private Context mContext;
    private int     REQUEST_CODE_GET_FACE = 10001;

    public ReportListAdapter(Context context, int layoutResId, List<ReportListInfo.ListBean> list) {
        super(layoutResId, list);
        mContext = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, ReportListInfo.ListBean item) {
        helper.setText(R.id.tv_No, null == item.getWorkno() ? "--" : item.getWorkno());
        helper.setText(R.id.tv_reportTime, "".equals(TimeUtil.changeDateTime2String(item.getCreate_time())) ? "--" : TimeUtil.changeDateTime2String(item.getCreate_time()));
        helper.setText(R.id.tv_startTime, "".equals(TimeUtil.subStrTime2Sec(item.getStarttime())) ? "--" : TimeUtil.subStrTime2Sec(item.getStarttime()));
        helper.setText(R.id.tv_closeTime, "".equals(TimeUtil.subStrTime2Sec(item.getEndtime())) ? "--" : TimeUtil.subStrTime2Sec(item.getEndtime()));

        View tv_open   = helper.getView(R.id.tv_open);
        View tv_close  = helper.getView(R.id.tv_close);
        View tv_person = helper.getView(R.id.tv_person);
        tv_open.setOnClickListener(v -> {
            //?????????????????????
            MyApplication.workID = item.getId();
            addPersonForOpenMachine(helper, 1);
        });
        if (null == item.getStarttime() || "".equals(item.getStarttime())) {
            tv_close.setBackground(mContext.getResources().getDrawable(R.drawable.bg_round_corner_red_gray));
        }else {
            tv_close.setBackground(mContext.getResources().getDrawable(R.drawable.bg_round_corner_red));
        }
        tv_close.setOnClickListener(v -> {
            if (null == item.getStarttime() || "".equals(item.getStarttime())) {
                //ToastUtils.showToast("?????????????????????");
                return;
            }
            //??????
            MyApplication.workID        = item.getId();
            MyApplication.workStartTime = item.getStarttime();
            closeMachine(helper);
        });
        tv_person.setOnClickListener(v -> {
            //??????????????????
            lookOrderPersons(helper, item, tv_person);
        });
    }

    /***??????
     * @param helper*/
    private void closeMachine(BaseViewHolder helper) {
        //????????????????????????
        Intent intent = new Intent(mContext, CameraPhotoActivity.class);
        intent.putExtra("ftype", 3);
        intent.putExtra("itemIndex", helper.getPosition());
        ((Activity) mContext).startActivityForResult(intent, REQUEST_CODE_GET_FACE);
    }

    /****??????????????????*/
    private void lookOrderPersons(BaseViewHolder helper, ReportListInfo.ListBean item, View tv_person) {
        MyApplication.workID = item.getId();
        PopupOpenHelper openHelper = new PopupOpenHelper(mContext, tv_person, R.layout.popup_order_person);
        openHelper.openPopupWindow(true, Gravity.CENTER);
        openHelper.setOnPopupViewClick((popupWindow, inflateView) -> {
            TextView     tv_addPerson = inflateView.findViewById(R.id.tv_addPerson);
            RecyclerView recy_person  = inflateView.findViewById(R.id.recy_person);
            //?????????????????????
            initRecyPersonList(recy_person, openHelper);
            tv_addPerson.setOnClickListener(v -> {
                //??????????????????
                addPersonForOpenMachine(helper, 2);
                openHelper.dismiss();
            });
            //??????????????????
            getExistPersonList(item);
        });
    }

    //??????????????????
    private void getExistPersonList(ReportListInfo.ListBean item) {
        mPersonList.clear();
        RequestParamsFM params = new RequestParamsFM();
        params.put("workid", item.getId());//b3943d02-67bd-4f35-a4cf-6534a0496d4c
        OkHttpUtils.getInstance().doGetWithParams(NetConfig.GDRYLIST, params, new OkHttpUtils.HttpCallBack() {
            @Override
            public void onError(Request request, IOException e) {
                ToastUtils.showToast("????????????");
            }

            @Override
            public void onSuccess(int code, String resbody) {
                if (code != 200) {
                    ToastUtils.showToast("?????????????????????");
                    return;
                }
                Gson                 gson                 = new Gson();
                ReportPersonListBean reportPersonListBean = gson.fromJson(resbody, ReportPersonListBean.class);
                mPersonList.addAll(reportPersonListBean.getList());
                personsShowAdapter.notifyDataSetChanged();
            }
        });
    }

    /****??????????????????
     * @param helper*/
    private void addPersonForOpenMachine(BaseViewHolder helper, int addType) {
        //????????????????????????
        Intent intent = new Intent(mContext, CameraPhotoActivity.class);
        intent.putExtra("ftype", 2);
        intent.putExtra("addType", addType);
        intent.putExtra("itemIndex", helper.getPosition());
        ((Activity) mContext).startActivityForResult(intent, REQUEST_CODE_GET_FACE);
    }

    private List<ReportPersonListBean.ListBean> mPersonList;
    private PersonsShowAdapter                  personsShowAdapter;

    /***?????????????????????
     * @param recy_person
     * @param openHelper*/
    private void initRecyPersonList(RecyclerView recy_person, PopupOpenHelper openHelper) {
        if (null == mPersonList) {
            mPersonList = new ArrayList();
        } else {
            mPersonList.clear();
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        recy_person.setLayoutManager(linearLayoutManager);
        personsShowAdapter = new PersonsShowAdapter(mContext, R.layout.recy_show_person, mPersonList);
        personsShowAdapter.setPopHelper(openHelper);
        recy_person.setAdapter(personsShowAdapter);
    }
}
