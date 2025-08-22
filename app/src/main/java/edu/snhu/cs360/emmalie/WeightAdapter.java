package edu.snhu.cs360.emmalie;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * RecyclerView adapter for showing weight entries (date + weight) with edit/delete actions.
 * Rows use layout: res/layout/item_weight.xml
 */
public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.VH> {

    public interface OnRowActionListener {
        void onEdit(@NonNull AppDatabaseHelper.WeightEntry entry);
        void onDelete(@NonNull AppDatabaseHelper.WeightEntry entry);
    }

    private final OnRowActionListener listener;

    // Data is always kept in kg (as stored in DB).
    private final List<AppDatabaseHelper.WeightEntry> items = new ArrayList<>();

    // Formatter provided by the Activity (switchable between kg/lbs).
    // Default is kg so this works even before the Activity sets one.
    private Function<Double, String> unitFormatter = kg -> String.format(Locale.US, "%.1f kg", kg);

    public WeightAdapter(@NonNull OnRowActionListener listener) {
        this.listener = listener;
    }

    /** Allow the Activity to set how we print a value in kg (e.g., "72.0 kg" or "158.7 lbs"). */
    @SuppressLint("NotifyDataSetChanged")
    public void setUnitFormatter(@NonNull Function<Double, String> formatter) {
        this.unitFormatter = formatter;
        notifyDataSetChanged();
    }

    /** Replace all rows (called after inserts/updates/deletes). */
    @SuppressLint("NotifyDataSetChanged")
    public void submitList(@NonNull List<AppDatabaseHelper.WeightEntry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged(); // simple & safe; list is small
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weight, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AppDatabaseHelper.WeightEntry row = items.get(position);

        holder.textDate.setText(row.date);
        holder.textWeight.setText(unitFormatter.apply(row.weight));

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(row));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(row));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** View holder for a single row. */
    public static class VH extends RecyclerView.ViewHolder {
        final TextView textDate;
        final TextView textWeight;
        final Button btnEdit;
        final Button btnDelete;

        public VH(@NonNull View itemView) {
            super(itemView);
            textDate   = itemView.findViewById(R.id.textDate);
            textWeight = itemView.findViewById(R.id.textWeight);
            btnEdit    = itemView.findViewById(R.id.btnEdit);
            btnDelete  = itemView.findViewById(R.id.btnDelete);
        }
    }
}
