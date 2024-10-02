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

/*
* - This class I reconfigured to be the launching class --- the main one if you will
* - This launches the list of notes and the + button.
*   - When the "+" button is clicked, it creates a note note and launches the "MainActivity.kt" with a blank configuration
*       - Also, the user gets to edit in a title before creating
*   - When an object from the list is clicks, it launches the "MainActivity.kt" with the configuration of the note
* - When a user presses and holds an object from the list, it deletes with confirmation, of course.
*   - Note (TODO): we can deciede if this press and hold is a good way
* - If this class is called by "MainActivity.kt" it takes the state of it newly configured and saves it
* */
public class TaskListView extends Activity {

    ListView listView;
    ArrayRepository repository = ArrayRepository.getInstance(); //this is a global class that holds all existing PartStates
    ToDoListAdapter toDoListAdapter;

    //OnCreate is always the first function to launch. Refer to Android activity lifecycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_task_list);

        //pretty much links objects from xml file above to these objects to make them editable
        listView = findViewById(R.id.listview);
        ImageView btnSearch = findViewById(R.id.btn_add);

        //This will not be null when "MainActivity.kt" calls this TaskListView activity and gives us
        // parameters for the new Partstate. If it already exists, get rid of it, but if it doesn't
        // add it to the list
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

        //calls list to populate the screen, basically
        if (!repository.getPartStates().isEmpty()) {
            toDoListAdapter = new ToDoListAdapter((Context) this, (ArrayList<PartState>) repository.getPartStates());
            listView.setAdapter(toDoListAdapter);
        }

        //listens for the "+" button to be clicked
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class); //creates intent to call "MainActivity.kt"

            //creates pop-up for user to set title up
            final EditText input = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Name of File?")
                    .setMessage(":)")
                    .setView(input)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //starts "MainActivity.kt" with no states, meaning it is new
                            String title = input.getText().toString();
                            intent.putExtra("blank", title);
                            startActivity(intent); //this starts the class
                            finish(); //this destroys this activity so both aren't running at the same time
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

        //if item is clicked, send Partstate data to "MainActivity.kt" to configure
        // (can't send the PArtState object *sad*, not allowed)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PartState currentPartState = repository.getPartStates().get(position);

            //adding all these are, like, sending optional parameters you can call when "MainActivity.kt" starts
            intent.putExtra("partId", currentPartState.getPartId());
            intent.putExtra("isReady", currentPartState.isReady());
            intent.putExtra("partType", currentPartState.getPartType().toString());
            intent.putExtra("partTitle", currentPartState.getTitle());
            intent.putExtra("partDate", currentPartState.getDateCreated());

            startActivity(intent); //starts "MainActivity.kt"
            finish(); //this destroys this activity so both aren't running at the same time
        });

        //this deletes item on the list with a long click
        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                PartState currentPartState = repository.getPartStates().get(pos);
                repository.removePartState(currentPartState); //removes from our array
                toDoListAdapter.remove(currentPartState); //removes from UI
                toDoListAdapter.notifyDataSetChanged(); //notifies UI that something has changed in the list and to update
                return true;
            }
        });
    }
}
