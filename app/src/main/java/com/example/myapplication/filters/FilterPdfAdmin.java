package com.example.myapplication.filters;

import android.widget.Filter;

import com.example.myapplication.adapters.AdapterCategory;
import com.example.myapplication.adapters.AdapterPdfAdmin;
import com.example.myapplication.models.ModelCategory;
import com.example.myapplication.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfAdmin extends Filter {
    // arraylist in which we want to search
    ArrayList<ModelPdf> filterList;
    // adapter in which filter has to be implemented
    AdapterPdfAdmin adapterPdfAdmin;

    public FilterPdfAdmin(ArrayList<ModelPdf> filterList, AdapterPdfAdmin adapterPdfAdmin) {
        this.filterList = filterList;
        this.adapterPdfAdmin = adapterPdfAdmin;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results=new FilterResults();
        // value should not be empty and null
        if(constraint!=null && constraint.length()>0)
        {
            // change uppercase or to lowercase,to avoid case senstivity
            constraint= constraint.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels=new ArrayList<>();

            for(int i=0;i<filterList.size();i++)
            {
                //validate
                if(filterList.get(i).getTitle().toUpperCase().contains(constraint))
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
        adapterPdfAdmin.pdfArrayList=(ArrayList<ModelPdf>)results.values;

        // notify changes
        adapterPdfAdmin.notifyDataSetChanged();
    }
}
