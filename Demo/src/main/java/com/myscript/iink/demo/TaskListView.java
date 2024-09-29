package com.myscript.iink.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.myscript.iink.demo.data.ArrayRepository;
import com.myscript.iink.demo.domain.PartType;
import com.myscript.iink.demo.ui.PartState;

import java.util.ArrayList;
import java.util.UUID;

//AI STATEMENT: this file used zero AI and was made by our team 100%

public class TaskListView extends Activity {

    ListView listView;
    ArrayRepository repository = ArrayRepository.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_task_list);


        listView = findViewById(R.id.listview);
        ImageView btnSearch = findViewById(R.id.btn_add);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String partTypeString = bundle.getString("partType");
            PartType partType = PartType.Companion.fromString(partTypeString);

            PartState partState = new PartState(
                    (String) bundle.get("partId"),
                    (Boolean) bundle.get("isReady"),
                    partType,
                    (UUID) bundle.get("partUUID")
            );

            Log.d("got part", partState.getPartUUID().toString());
            repository.addPartState(partState);
        }

        if (!repository.getPartStates().isEmpty()) {
            ToDoListAdapter toDoListAdapter = new ToDoListAdapter((Context) this, (ArrayList<PartState>) repository.getPartStates());
            listView.setAdapter(toDoListAdapter);
        }

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("blank", "yeah");
            startActivity(intent);
            finish();
        });
    }
}
