package cn.lxl.notes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.lxl.notes.db.sqlhelper;


/**
 * Created by Administrator on 2016/3/26.
 */
public class LoginAty extends Activity {
    private   EditText edtAccount, edtPass;
    private   String account;
    private  String password;
    private sqlhelper sh;
    Button btnlogTitle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        init();

    }

    public void init(){
        edtAccount=(EditText)findViewById(R.id.edtAccount);
        edtPass=(EditText)findViewById(R.id.edtPass);
        findViewById(R.id.btnloginLogin).setOnClickListener(btn_handler);
        findViewById(R.id.btnCancleLogin).setOnClickListener(btn_handler);
    }

  private View.OnClickListener btn_handler=new View.OnClickListener() {
      @Override
      public void onClick(View v) {
          switch (v.getId()){
              case R.id.btnloginLogin:
                  btnLogin();
                  break;
              case  R.id.btnCancleLogin:
                  finish();
                  break;
          }
      }
  };
    public void btnLogin() {
        if (TextUtils.isEmpty(edtAccount.getText()) || TextUtils.isEmpty(edtPass.getText())) {
            Toast.makeText(getApplicationContext(), "用户名或密码不能为空!!", Toast.LENGTH_SHORT).show();
        } else {
            View view= LayoutInflater.from(LoginAty.this).inflate(R.layout.activity_main, null);
            btnlogTitle=(Button) view.findViewById(R.id.btnLogtitle);
            account = edtAccount.getText().toString();
            password = edtPass.getText().toString();
            Thread thread = new Thread(runable);
            thread.start();
            //等待线程结果
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //验证通过则设置同步按钮可见
            if (hasresult){
                btnlogTitle.setText("云同步");
                Intent intent=new Intent(LoginAty.this,MainActivity.class);
                startActivity(intent);
            }else {
                Toast.makeText(LoginAty.this,"密码或用户名不正确",Toast.LENGTH_SHORT).show();
            }
        }
    }
    Boolean hasresult=false;
    Runnable runable=new Runnable() {
        @Override
        public void run() {
            sh=new sqlhelper();
            hasresult=sh.openMynote("select * from account where username ="+"'"+account+"'"+"and apassword="+"'"+password+"'");
            Log.d("run()",hasresult+"");
        }
    };

}
