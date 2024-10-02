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

public class ToDoListAdapter extends ArrayAdapter<PartState> {

    public ToDoListAdapter(@NonNull Context context, ArrayList<PartState> arrayList) {
        super(context, 0, arrayList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View currentItemView = convertView;

        if (currentItemView == null) {
            currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_item, parent, false);
        }

        PartState currentNumberPosition = getItem(position);

        TextView textView = currentItemView.findViewById(R.id.title);
        textView.setText(currentNumberPosition.getTitle());

        TextView textViewTwo = currentItemView.findViewById(R.id.date);
        textViewTwo.setText(currentNumberPosition.getDateCreated());

        return currentItemView;
    }
}
