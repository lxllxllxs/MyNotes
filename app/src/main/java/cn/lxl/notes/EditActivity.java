package cn.lxl.notes;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.lxl.notes.db.NotesDB;

public class EditActivity extends ListActivity {
	private  final  String TAG="MyNote";//debug
	private int noteId = -1;
	private EditText etName,etContent;
	private MediaAdapter adapter;
	private NotesDB db;
	private SQLiteDatabase dbRead,dbWrite;
	private String currentPath = null;
	private ListView lv;
	public static final int REQUEST_CODE_GET_PHOTO = 1;
	public static final int REQUEST_CODE_GET_VIDEO = 2;
	public static final String EXTRA_NOTE_ID = "noteId";
	public static final String EXTRA_NOTE_NAME = "noteName";
	public static final String EXTRA_NOTE_CONTENT = "noteContent";

	private OnClickListener btnClickHandler=new OnClickListener() {
		Intent i;File f;
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btnAddPhoto:
				i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				f = new File(getMediaDir(), System.currentTimeMillis()+".jpg");
				if (!f.exists()) {
					try {
						f.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				currentPath = f.getAbsolutePath();
				i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
				startActivityForResult(i, REQUEST_CODE_GET_PHOTO);
				break;
			case R.id.btnAddVideo:
				
				i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
				f = new File(getMediaDir(), System.currentTimeMillis()+".mp4");
				if (!f.exists()) {
					try {
						f.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				currentPath = f.getAbsolutePath();
				i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
				startActivityForResult(i, REQUEST_CODE_GET_VIDEO);
				break;
			case R.id.btnSave:
				if(TextUtils.isEmpty(etName.getText())){
					Toast.makeText(getApplicationContext(),"标题不能为空!!",Toast.LENGTH_SHORT).show();
				}else {
				saveMedia(saveNote());
				setResult(RESULT_OK);
				finish();}
				break;
			case R.id.btnCancel:
				setResult(RESULT_CANCELED);
				finish();
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.aty_eidt_note);
		lv=(ListView)findViewById(android.R.id.list);
		lv.setOnItemLongClickListener(editlongClickListener);
		db = new NotesDB(this);
		dbRead = db.getReadableDatabase();
		dbWrite = db.getWritableDatabase();
		adapter = new MediaAdapter(this);
		setListAdapter(adapter);

		etName = (EditText) findViewById(R.id.etName);
		etContent = (EditText) findViewById(R.id.etContent);
		//如果是编辑则有值(cv)传人，没有值传入时noteId设为-1
		noteId = getIntent().getIntExtra(EXTRA_NOTE_ID, -1);
		
		if (noteId>-1) {
			etName.setText(getIntent().getStringExtra(EXTRA_NOTE_NAME));
			etContent.setText(getIntent().getStringExtra(EXTRA_NOTE_CONTENT));
			Cursor c = dbRead.query(NotesDB.TABLE_NAME_MEDIA, null, NotesDB.COLUMN_NAME_MEDIA_OWNER_NOTE_ID+"=?", new String[]{noteId+""}, null, null, null);
			while(c.moveToNext()){
				//从数据库中装入数据，将noteId存进data中
				adapter.add(new MediaListCellData(c.getString(c.getColumnIndex(NotesDB.COLUMN_NAME_MEDIA_PATH)),c.getInt(c.getColumnIndex(NotesDB.COLUMN_NAME_ID))));
			}
			//如果是编辑则加入长按删除事件
			adapter.notifyDataSetChanged();
		}
		
		findViewById(R.id.btnSave).setOnClickListener(btnClickHandler);
		findViewById(R.id.btnCancel).setOnClickListener(btnClickHandler);
		findViewById(R.id.btnAddPhoto).setOnClickListener(btnClickHandler);
		findViewById(R.id.btnAddVideo).setOnClickListener(btnClickHandler);
	}



	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//通过postiong获取到celldata,再从celldata中得到id，可对进行操作
		MediaListCellData data = adapter.getItem(position);
		Intent i;
		
		switch (data.type) {
		case MediaType.PHOTO:
			i = new Intent(this,    PhotoViewer.class);
			i.putExtra(PhotoViewer.EXTRA_PATH, data.path);
			startActivity(i);
			break;
		case MediaType.VIDEO:
			i = new Intent(this, VideoViewer.class);
			i.putExtra(VideoViewer.EXTRA_PATH, data.path);
			startActivity(i);
			break;
		}
		
		super.onListItemClick(l, v, position, id);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		System.out.println(data);
		
		switch (requestCode) {
		case REQUEST_CODE_GET_PHOTO://吗
		case REQUEST_CODE_GET_VIDEO:
			if (resultCode==RESULT_OK) {
				adapter.add(new MediaListCellData(currentPath));
				adapter.notifyDataSetChanged();
			}
			break;
		default:
			break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
	public File getMediaDir(){
		File dir = new File(Environment.getExternalStorageDirectory(), "NotesMedia");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
	
	public void saveMedia(int noteId){
		
		MediaListCellData data;
		ContentValues cv;
		
		for (int i = 0; i < adapter.getCount(); i++) {
			data = adapter.getItem(i);
			
			if (data.id<=-1) {
				cv = new ContentValues();
				cv.put(NotesDB.COLUMN_NAME_MEDIA_PATH, data.path);
				cv.put(NotesDB.COLUMN_NAME_MEDIA_OWNER_NOTE_ID, noteId);
				dbWrite.insert(NotesDB.TABLE_NAME_MEDIA, null, cv);
			}
		}
		
	}
	
	public int saveNote(){

		ContentValues cv = new ContentValues();
		cv.put(NotesDB.COLUMN_NAME_NOTE_NAME, etName.getText().toString());
		cv.put(NotesDB.COLUMN_NAME_NOTE_CONTENT, etContent.getText().toString());
		cv.put(NotesDB.COLUMN_NAME_NOTE_DATE, new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
		
		if (noteId>-1) {
			dbWrite.update(NotesDB.TABLE_NAME_NOTES, cv, NotesDB.COLUMN_NAME_ID+"=?", new String[]{noteId+""});
			return noteId;
		}else{
			return (int) dbWrite.insert(NotesDB.TABLE_NAME_NOTES, null, cv);
		}
	}
	
	
	@Override
	protected void onDestroy() {
		dbRead.close();
		dbWrite.close();
		super.onDestroy();
	}

	public AdapterView.OnItemLongClickListener editlongClickListener=new AdapterView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			MediaListCellData data = adapter.getItem(position);

			//Toast.makeText(EditActivity.this, "" + data.id,Toast.LENGTH_SHORT).show();
			getDialog(data.id, position);
			return true;
		}
	};


	//删除添加的图片和录像
	public  void deleteItem(int position){
		String sql="delete from media where _id ="+position;
		dbWrite.execSQL(sql);
	}

	public  void getDialog(final int dataId,final  int position){
		AlertDialog.Builder builder=new AlertDialog.Builder(EditActivity.this);
		builder.setTitle("注意");
		builder.setMessage("确定要删除该项吗？");
		builder.setCancelable(false);
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//要执行的删除操作放这里
				adapter.remove(position);//从ListView中删除
				deleteItem(dataId);//从数据库中删除
				adapter.notifyDataSetChanged();//刷新界面
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		builder.show();
	}

}
