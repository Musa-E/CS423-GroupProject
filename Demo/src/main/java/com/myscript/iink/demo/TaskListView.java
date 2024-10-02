package com.myscript.iink.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.myscript.iink.demo.data.ArrayRepository;
import com.myscript.iink.demo.domain.PartType;
import com.myscript.iink.demo.ui.PartState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

//AI STATEMENT: this file used zero AI and was made by our team 100%

public class TaskListView extends Activity {

    ListView listView;
    ArrayRepository repository = ArrayRepository.getInstance();
    ToDoListAdapter toDoListAdapter;


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
                    (String) bundle.get("partDate"),
                    (String) bundle.get("partTitle")
            );

            if(!repository.checkForPart(partState)){
                repository.addPartState(partState);
            }
        }

        if (!repository.getPartStates().isEmpty()) {
            toDoListAdapter = new ToDoListAdapter((Context) this, (ArrayList<PartState>) repository.getPartStates());
            listView.setAdapter(toDoListAdapter);
        }

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                    final EditText input = new EditText(this);

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Name of File?")
                            .setMessage(":)")
                            .setView(input)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String title = input.getText().toString();
                                    intent.putExtra("blank", title);

                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    return;
                                }
                            });
                    builder.show();
        });

        listView.setLongClickable(true);


        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PartState currentPartState = repository.getPartStates().get(position);
            intent.putExtra("partId", currentPartState.getPartId());
            intent.putExtra("isReady", currentPartState.isReady());
            intent.putExtra("partType", currentPartState.getPartType().toString());
            intent.putExtra("partTitle", currentPartState.getTitle());
            intent.putExtra("partDate", currentPartState.getDateCreated());

            startActivity(intent);
            finish();
        });

        //this deletes item on the list with a long click
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                PartState currentPartState = repository.getPartStates().get(pos);
                repository.removePartState(currentPartState);

                toDoListAdapter.remove(currentPartState);

                toDoListAdapter.notifyDataSetChanged();

                return true;
            }
        });
    }
}
