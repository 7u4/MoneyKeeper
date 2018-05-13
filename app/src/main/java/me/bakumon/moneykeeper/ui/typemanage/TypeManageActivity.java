package me.bakumon.moneykeeper.ui.typemanage;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.bakumon.moneykeeper.Injection;
import me.bakumon.moneykeeper.R;
import me.bakumon.moneykeeper.Router;
import me.bakumon.moneykeeper.base.BaseActivity;
import me.bakumon.moneykeeper.database.entity.RecordType;
import me.bakumon.moneykeeper.databinding.ActivityTypeManageBinding;
import me.bakumon.moneykeeper.ui.typesort.TypeSortActivity;
import me.bakumon.moneykeeper.utill.ToastUtils;
import me.bakumon.moneykeeper.viewmodel.ViewModelFactory;
import me.drakeet.floo.Floo;

/**
 * 类型管理
 *
 * @author bakumon https://bakumon.me
 * @date 2018/5/3
 */
public class TypeManageActivity extends BaseActivity {

    private static final String TAG = TypeManageActivity.class.getSimpleName();
    public static final String KEY_TYPE = "TypeManageActivity.key_type";

    private ActivityTypeManageBinding mBinding;
    private TypeManageViewModel mViewModel;
    private TypeManageAdapter mAdapter;
    private List<RecordType> mRecordTypes;

    private int mCurrentType;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_type_manage;
    }

    @Override
    protected void onInit(@Nullable Bundle savedInstanceState) {
        mBinding = getDataBinding();
        ViewModelFactory viewModelFactory = Injection.provideViewModelFactory(this);
        mViewModel = ViewModelProviders.of(this, viewModelFactory).get(TypeManageViewModel.class);

        initView();
        initData();
    }

    private void initView() {
        mCurrentType = getIntent().getIntExtra(KEY_TYPE, RecordType.TYPE_OUTLAY);

        mBinding.titleBar.ibtClose.setOnClickListener(v -> finish());
        mBinding.titleBar.setTitle(getString(R.string.text_title_type_manage));
        mBinding.titleBar.setRightText(getString(R.string.text_button_sort));
        mBinding.titleBar.tvRight.setOnClickListener(v ->
                Floo.navigation(this, Router.TYPE_SORT)
                        .putExtra(TypeSortActivity.KEY_TYPE, mCurrentType)
                        .start());

        mBinding.rvType.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new TypeManageAdapter(null);
        mBinding.rvType.setAdapter(mAdapter);

        mAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            showDeleteDialog(mAdapter.getData().get(position).name, mAdapter.getData().get(position));
            return false;
        });

        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            ToastUtils.show("点击了" + position);
        });

        mBinding.typeChoice.rgType.setOnCheckedChangeListener((group, checkedId) -> {
            mCurrentType = checkedId == R.id.rb_outlay ? RecordType.TYPE_OUTLAY : RecordType.TYPE_INCOME;
            mAdapter.setNewData(mRecordTypes, mCurrentType);
        });

    }

    private void showDeleteDialog(String typeName, RecordType recordType) {
        String msg = "删除 " + typeName + " 分类后，将无法在记账页选择该分类，该分类下原有账单仍保持不变";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.text_dialog_delete_type);
        builder.setMessage(msg);
        builder.setNegativeButton(R.string.text_button_cancel, null);

        builder.setPositiveButton(R.string.text_button_affirm_delete, (dialog, which) -> {
            deleteType(recordType);
        });

        builder.create();
        builder.show();
    }

    private void deleteType(RecordType recordType) {
        mDisposable.add(mViewModel.deleteRecordType(recordType).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                        },
                        throwable -> {
                            ToastUtils.show("删除失败");
                            Log.e(TAG, "类型删除失败", throwable);
                        }
                ));
    }

    private void initData() {
        mDisposable.add(mViewModel.getAllRecordTypes().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((recordTypes) -> {
                            mRecordTypes = recordTypes;
                            int id = mCurrentType == RecordType.TYPE_OUTLAY ? R.id.rb_outlay : R.id.rb_income;
                            mBinding.typeChoice.rgType.clearCheck();
                            mBinding.typeChoice.rgType.check(id);
                        },
                        throwable ->
                                Log.e(TAG, "获取类型数据失败", throwable)
                )
        );
    }
}
