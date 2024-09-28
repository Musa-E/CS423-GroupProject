package com.myscript.iink.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

//AI STATEMENT: this file used zero AI and was made by our team 100%

public class TaskListView extends Activity {

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_menu_task_list);

        ImageView btnSearch= findViewById(R.id.btn_add);
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        });



    }
}
