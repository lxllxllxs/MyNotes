package cn.lxl.notes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/3/24.
 */
public class MediaAdapter extends BaseAdapter{

    private Context context;

    private List<MediaListCellData> list = new ArrayList< MediaListCellData>();

        public MediaAdapter(Context context) {
            this.context = context;
        }

        public void add(MediaListCellData data){
            list.add(data);
        }
        @Override
        public int getCount() {
            return list.size();
        }
        @Override
        public MediaListCellData getItem(int position) {
            return list.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView==null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.media_list_cell, null);
            }

            MediaListCellData data = getItem(position);

            ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivIcon);
            TextView tvPath = (TextView) convertView.findViewById(R.id.tvPath);

            ivIcon.setImageResource(data.iconId);
            tvPath.setText(data.path);
			tvPath.setTextSize(11);
            return convertView;
        }

		public void remove(int postion){
			list.remove(postion);
		}


    }

     class MediaListCellData{
		 int type = 0;
		 int id = -1;
		 String path = "";
		 int iconId = R.drawable.ic_launcher;

		 public MediaListCellData(String path) {
            this.path = path;

            if (path.endsWith(".jpg")) {
                iconId = R.drawable.icon_photo;
                type = MediaType.PHOTO;
            }else if (path.endsWith(".mp4")) {
                iconId = R.drawable.icon_video;
                type = MediaType.VIDEO;
            }
        }

        public MediaListCellData(String path,int id) {
            this(path);

            this.id = id;
        }

    }
     class MediaType{
        static final int PHOTO = 1;
        static final int VIDEO = 2;
    }

