package cn.lxl.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import cn.lxl.notes.db.NotesDB;


public class MainActivity extends ListActivity {
    private long mExitTime=0;
	private SimpleCursorAdapter adapter = null;
	private NotesDB db;
	private SQLiteDatabase dbRead,dbWrite;
	public static final int REQUEST_CODE_ADD_NOTE = 1;
	public static final int REQUEST_CODE_EDIT_NOTE = 2;
	public static final int NOT_DELETE_ALL = 0;
	public static final int DELETE_ALL = 1;
	Boolean isSuccess;
	Toolbar tb;
	//记录数据库文件的位置
	final String filepath="data/data/com.lxl.notes/databases/notes.db";
	//记录备份到本地时的位置
	final String exportPath= Environment.getExternalStorageDirectory()+"/notes.db";
	private OnClickListener btn_clickHandler=new OnClickListener() {
		@Override
		public void onClick(View v) {
					//跳转到编辑界面
					startActivityForResult(new Intent(MainActivity.this, EditActivity.class), REQUEST_CODE_ADD_NOTE);
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		toobarInit();//设置工具栏
		db = new NotesDB(this);
		//获得数据库的操作对像实例
		dbRead = db.getReadableDatabase();
		dbWrite=db.getWritableDatabase();
		//adapter加载布局、数据
		adapter = new SimpleCursorAdapter(this, R.layout.notes_list_cell, null, new String[]{NotesDB.COLUMN_NAME_NOTE_NAME,NotesDB.COLUMN_NAME_NOTE_DATE}, new int[]{R.id.tvName,R.id.tvDate});
		//把Adapter设置为ListView的适配器
		setListAdapter(adapter);
		ListView lv=(ListView)findViewById(android.R.id.list);
		//设置ListView的长按Item事件监听器
		lv.setOnItemLongClickListener(itemLongClickListener);
		//刷新界面
		refreshNotesListView();
		//绑定添加按钮的响应时间
		findViewById(R.id.btnAddNote).setOnClickListener(btn_clickHandler);
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Cursor c = adapter.getCursor();
		c.moveToPosition(position);

		Intent i = new Intent(MainActivity.this,EditActivity.class);
		//获得点击Item的数据并绑定到Intent传到EditActivity
		i.putExtra(EditActivity.EXTRA_NOTE_ID, c.getInt(c.getColumnIndex(NotesDB.COLUMN_NAME_ID)));
		i.putExtra(EditActivity.EXTRA_NOTE_NAME, c.getString(c.getColumnIndex(NotesDB.COLUMN_NAME_NOTE_NAME)));
		i.putExtra(EditActivity.EXTRA_NOTE_CONTENT, c.getString(c.getColumnIndex(NotesDB.COLUMN_NAME_NOTE_CONTENT)));
		//启动Intent
		startActivityForResult(i, REQUEST_CODE_EDIT_NOTE);
		
		super.onListItemClick(l, v, position, id);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		switch (requestCode) {
		case REQUEST_CODE_ADD_NOTE:
		case REQUEST_CODE_EDIT_NOTE:
			if (resultCode == Activity.RESULT_OK) {
				refreshNotesListView();
			}
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	//刷新adapter里的数据
	public void refreshNotesListView(){
		adapter.changeCursor(dbRead.query(NotesDB.TABLE_NAME_NOTES, null, null, null, null, null, null));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//toolbar加载菜单栏的布局
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
    //连续点击两次返回键才完全退出
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

	//导出到本地的方法
	public  void exportFile(){

		try {
			FileInputStream fis=new FileInputStream(new File(filepath));
			FileOutputStream fos=new FileOutputStream(new File(exportPath));
			byte [] buff=new byte[1024];
			int i=fis.read(buff);
			while(i!=-1){
				fos.write(buff,0,i);
				i=fis.read(buff);
			}
			fis.close();
			fos.close();
			Toast.makeText(getApplicationContext(),"导出成功！",Toast.LENGTH_SHORT).show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public  void AlertDialog(Context context){
		AlertDialog.Builder builder=new AlertDialog.Builder(context);
		builder.setTitle("注意");
		builder.setMessage("确定要清空所有日志记录吗？");
		builder.setCancelable(false);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				delAll(DELETE_ALL,null,null);
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		builder.show();
	}
	//删除数据的方法
	public void delAll(int i,String sql,String sql2){
		//当i为1时删除所有数据，否则按sql语句进行
		if (i==DELETE_ALL){
		dbWrite.delete("notes", null, null);
		dbWrite.delete("media", null, null);
		Toast.makeText(MainActivity.this,"已清空所有数据",Toast.LENGTH_SHORT).show();
		refreshNotesListView();
		}else{
			dbWrite.execSQL(sql);
			dbWrite.execSQL(sql2);
			Toast.makeText(MainActivity.this,"已删除",Toast.LENGTH_SHORT).show();
			refreshNotesListView();
		}

	}

	public AdapterView.OnItemLongClickListener itemLongClickListener=new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
			final  String sql="delete from notes where _id="+id;
			final  String sql2="delete from media where note_id="+id;
			builder.setTitle("注意");
			builder.setMessage("确定要删除该记录吗？");
			builder.setCancelable(false);
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					delAll(NOT_DELETE_ALL,sql,sql2);
				}
			});
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			builder.show();

			return true;//设为true则只响应长按事件而不响应点击事件
		}
	};

	//本地恢复的方法
	private void importFile() {
		File backfile=new File(exportPath);
		if(backfile.exists()&&backfile.isFile()){
			try {
				FileInputStream fis=new FileInputStream(backfile);
				FileOutputStream fos=new FileOutputStream(new File(filepath));
				int count=0;
				byte[] buff=new byte[1024];
				while((count=fis.read(buff))!=-1){
					fos.write(buff,0,count);
				}
				fis.close();
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}


	//工具栏toolbar的初始化设置，绑定子菜单项的监听
	private void toobarInit() {
		tb=(Toolbar)findViewById(R.id.toolbar);
		tb.setTitle("MyNote");
		tb.setLogo(R.mipmap.title);
		tb.inflateMenu(R.menu.main);
		tb.setBackgroundColor(0xffEEEEEE);
		tb.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.item2) {
					exportFile();
				}
				if (item.getItemId() ==  R.id.item1) {
					AlertDialog(MainActivity.this);
				}
				if (item.getItemId() ==  R.id.item3) {
					importFile();
					Toast.makeText(MainActivity.this, "导入成功", Toast.LENGTH_SHORT).show();
					refreshNotesListView();
				}
				if(item.getItemId()==R.id.item4){
						postFile();
				}




				return  true;
			}
		});

	}

	//利用AsnycHttpClient框架上传文件
	public Boolean postFile()  {
		String url="http://192.168.0.109/";
		String testPath="/data/data/com.lxl.notes/databases/415.jpg";
		File file = new File(filepath);
		if (file.exists() && file.length() > 0) {
			AsyncHttpClient client = new AsyncHttpClient();
			RequestParams params = new RequestParams();
			client.setTimeout(20000);
			try {
				params.put("note", file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			client.post(url,params,new AsyncHttpResponseHandler(){
				@Override
				public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, Throwable error) {
					Toast.makeText(MainActivity.this,"文件上传失败",Toast.LENGTH_SHORT).show();
				}

				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
					if (statusCode==200) {
						Toast.makeText(MainActivity.this, "文件上传成功" + statusCode, Toast.LENGTH_SHORT).show();
					}
					}
			});
		}else {
			Toast.makeText(MainActivity.this,"文件不存在",Toast.LENGTH_SHORT).show();
		}
		return isSuccess;
	}



	//利用okHttp框架上传文件
	/*public void run(String url, String filePath) throws Exception {
		// Use the imgur image upload API as documented at https://api.imgur.com/endpoints/image

		   String IMGUR_CLIENT_ID = "9199fdef135c122";
		   MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

		  OkHttpClient client = new OkHttpClient();
		File file = new File(filePath);

		RequestBody requestBody = new MultipartBuilder()
				.type(MultipartBuilder.FORM)
				.addFormDataPart("title", "Square Logo")
				.addFormDataPart("image", file.getName(),
						RequestBody.create(MEDIA_TYPE_PNG, file))
				.build();

		Request request = new Request.Builder()
				.header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
				.url(url)
				.post(requestBody)
				.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

		System.out.println(response.body().string());
		}

*/

//download
public static void downLoad(String path,Context context)throws Exception
{
	URL url = new URL(path);
	InputStream is = url.openStream();
	//截取最后的文件名
	String end = path.substring(path.lastIndexOf("."));
	//打开手机对应的输出流,输出到文件中
	OutputStream os = context.openFileOutput("Cache_"+System.currentTimeMillis()+end, Context.MODE_PRIVATE);
	byte[] buffer = new byte[1024];
	int len = 0;
	//从输入六中读取数据,读到缓冲区中
	while((len = is.read(buffer)) > 0)
	{
		os.write(buffer,0,len);
	}
	//关闭输入输出流
	is.close();
	os.close();
}







}//last
