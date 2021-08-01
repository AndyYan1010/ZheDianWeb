package com.botian.zhedian.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.botian.zhedian.R;
import com.botian.zhedian.activity.CameraPhotoActivity;
import com.botian.zhedian.bean.CloseRecordBean;
import com.botian.zhedian.utils.TimeUtil;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class PersonsCloseAdapter extends BaseQuickAdapter<CloseRecordBean.ListBean, BaseViewHolder> {
    private Context mContext;
    private int     REQUEST_CODE_GET_FACE = 10001;

    public PersonsCloseAdapter(Context context, int layoutResId, List<CloseRecordBean.ListBean> list) {
        super(layoutResId, list);
        mContext = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, CloseRecordBean.ListBean item) {
        helper.setText(R.id.tv_line, (null == item.getWorkno() || "".equals(item.getWorkno())) ? "" : item.getWorkno());
        helper.setText(R.id.tv_creatTime, TimeUtil.subStrTime2Sec(item.getCreatetime()));
        helper.setText(R.id.tv_startTime, TimeUtil.subStrTime2Sec(item.getStarttime()));
        helper.setText(R.id.tv_name, null == item.getUsers() ? "" : item.getUsers());

        View tv_delPerson = helper.getView(R.id.tv_delPerson);
        //View img_add = helper.getView(R.id.img_add);
        tv_delPerson.setOnClickListener(v -> {
            //删除关机人员
            delPersonForOpenMachine(helper.getPosition());
        });
    }

    /****删除关机人员
     * @param position*/
    private void delPersonForOpenMachine(int position) {
        //跳转人脸识别界面
        Intent intent = new Intent(mContext, CameraPhotoActivity.class);
        intent.putExtra("ftype", 3);
        intent.putExtra("itemIndex", position);
        ((Activity) mContext).startActivityForResult(intent, REQUEST_CODE_GET_FACE);
    }
}
