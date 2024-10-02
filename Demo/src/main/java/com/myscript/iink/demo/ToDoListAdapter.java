package com.myscript.iink.demo;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.myscript.iink.demo.ui.PartState;

import java.util.ArrayList;

//AI STATEMENT: this file used zero AI and was made by our team 100%

/*
 This class takes, adds, configures (whatever you want to call it) the list for the UI. It makes the list change and stuff
 (and also lets you handle each list item sep, with different variables (like calling the PartState you want with the click))
 */
public class ToDoListAdapter extends ArrayAdapter<PartState> {

    //forced function when you call "extends ArrayAdapter"
    public ToDoListAdapter(@NonNull Context context, ArrayList<PartState> arrayList) {
        super(context, 0, arrayList);
    }

    //This function is great because it takes each individual item in the list and configures it to each spot
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View currentItemView = convertView; //the view is, basically, the screen

        //if nothing is on the screen, aka no list, then do this
        if (currentItemView == null) {
            /*
            - getContext(): gets the info of the activity we are inserting the list in;
            - R.layout.list_view_item: configures each item to that XML file for each individual list item
             */
            currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_item, parent, false);
        }

        //gets position of the list that you clicked on and turns it into an object
        PartState currentNumberPosition = getItem(position);

        //configures title of text to that object's
        TextView textView = currentItemView.findViewById(R.id.title);
        textView.setText(currentNumberPosition.getTitle());

        //configures date of text to that object's
        TextView textViewTwo = currentItemView.findViewById(R.id.date);
        textViewTwo.setText(currentNumberPosition.getDateCreated());

        return currentItemView;
    }
}
