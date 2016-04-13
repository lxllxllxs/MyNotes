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
import android.util.Log;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
	final String filepath="/data/data/com.lxl.notes/databases/notes.db";
	final String exportPath= Environment.getExternalStorageDirectory()+"/notes.db";
	private OnClickListener btn_clickHandler=new OnClickListener() {
		@Override
		public void onClick(View v) {

					startActivityForResult(new Intent(MainActivity.this, EditNote.class), REQUEST_CODE_ADD_NOTE);
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//fullandnotitle();//设置全屏无标题
		setContentView(R.layout.activity_main);

		db = new NotesDB(this);
		dbRead = db.getReadableDatabase();
		dbWrite=db.getWritableDatabase();
		adapter = new SimpleCursorAdapter(this, R.layout.notes_list_cell, null, new String[]{cn.lxl.notes.db.NotesDB.COLUMN_NAME_NOTE_NAME, cn.lxl.notes.db.NotesDB.COLUMN_NAME_NOTE_DATE}, new int[]{R.id.tvName,R.id.tvDate});
		setListAdapter(adapter);
		ListView lv=(ListView)findViewById(android.R.id.list);
		lv.setOnItemLongClickListener(itemLongClickListener);
		refreshNotesListView();
		Log.d("exportpath",exportPath);
		findViewById(R.id.btnAddNote).setOnClickListener(btn_clickHandler);
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		Cursor c = adapter.getCursor();
		c.moveToPosition(position);
		
		Intent i = new Intent(MainActivity.this,EditNote.class);
		i.putExtra(EditNote.EXTRA_NOTE_ID, c.getInt(c.getColumnIndex(NotesDB.COLUMN_NAME_ID)));
		i.putExtra(EditNote.EXTRA_NOTE_NAME, c.getString(c.getColumnIndex(NotesDB.COLUMN_NAME_NOTE_NAME)));
		i.putExtra(EditNote.EXTRA_NOTE_CONTENT, c.getString(c.getColumnIndex(NotesDB.COLUMN_NAME_NOTE_CONTENT)));
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
	
	public void refreshNotesListView(){
		adapter.changeCursor(dbRead.query(NotesDB.TABLE_NAME_NOTES, null, null, null, null, null, null));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
    //连续点击两次返回键才完全退出
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 1000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void fullandnotitle(){
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*set it to be full screen*/
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

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
				delAll(DELETE_ALL,null);
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		builder.show();
	}

	public void delAll(int i,String sql){
		//当i为1时删除所有数据，否则按sql语句进行
		if (i==1){
		dbWrite.delete("notes", null, null);
		dbWrite.delete("media", null, null);
		Toast.makeText(MainActivity.this,"已清空所有数据",Toast.LENGTH_SHORT).show();
		refreshNotesListView();
		}else{
			dbWrite.execSQL(sql);
			Toast.makeText(MainActivity.this,"已删除",Toast.LENGTH_SHORT).show();
			refreshNotesListView();
		}

	}

	public AdapterView.OnItemLongClickListener itemLongClickListener=new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
			final  String sql="delete from notes ,media where _id="+id+"or note_id="+id;
			builder.setTitle("注意");
			builder.setMessage("确定要删除该记录吗？");
			builder.setCancelable(false);
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					delAll(NOT_DELETE_ALL,sql);
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


	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		//Toast.makeText(this,"asd",Toast.LENGTH_SHORT).show();
		menu.add(0,1,2,"清空所有数据");
		menu.add(0,2,2,"备份到内存卡");
		menu.add(0,3,2, "从sd卡导入");
		return super.onCreatePanelMenu(featureId, menu);

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId()==2){
			exportFile();
		}if (item.getItemId()==1) {
			AlertDialog(MainActivity.this);
		}if(item.getItemId()==3){
			importFile();
			Toast.makeText(MainActivity.this,"导入成功",Toast.LENGTH_SHORT).show();
			refreshNotesListView();
		}
		return super.onMenuItemSelected(featureId, item);
	}

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
}//last
