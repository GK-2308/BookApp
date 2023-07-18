package com.example.myapplication.filters;

import android.widget.Filter;

import com.example.myapplication.adapters.AdapterCategory;
import com.example.myapplication.models.ModelCategory;

import java.util.ArrayList;

public class FilterCategory extends Filter {
    // arraylist in which we want to search
    ArrayList<ModelCategory> filterList;
    // adapter in which filter has to be implemented
    AdapterCategory adapterCategory;

    public FilterCategory(ArrayList<ModelCategory> filterList, AdapterCategory adapterCategory) {
        this.filterList = filterList;
        this.adapterCategory = adapterCategory;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results=new FilterResults();
        // value should not be empty and null
        if(constraint!=null && constraint.length()>0)
        {
            // change uppercase or to lowercase,to avoid case senstivity
            constraint= constraint.toString().toUpperCase();
            ArrayList<ModelCategory> filteredModels=new ArrayList<>();

            for(int i=0;i<filterList.size();i++)
            {
                //validate
                if(filterList.get(i).getCategory().toUpperCase().contains(constraint))
                {
                    // add to filteredList
                    filteredModels.add(filterList.get((i)));
                }
            }

            results.count=filteredModels.size();
            results.values=filteredModels;
        }
        else
        {
            results.count=filterList.size();
            results.values=filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        // apply filtered changes
        adapterCategory.categoryArrayList=(ArrayList<ModelCategory>)results.values;

        // notify changes
        adapterCategory.notifyDataSetChanged();
    }
}
