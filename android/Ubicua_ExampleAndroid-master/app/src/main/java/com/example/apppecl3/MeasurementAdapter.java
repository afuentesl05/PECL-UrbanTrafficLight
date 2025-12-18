package com.example.apppecl3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder> {

    private final List<MeasurementDto> data;

    public MeasurementAdapter(List<MeasurementDto> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public MeasurementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_measurement, parent, false);
        return new MeasurementViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MeasurementViewHolder holder, int position) {
        MeasurementDto m = data.get(position);

        holder.textTimestamp.setText(TimeUtils.toMadrid(m.getTimestamp()));

        Integer rem = m.getTimeRemainingSeconds();
        String estado = m.getCurrentState() != null ? m.getCurrentState() : "-";
        String remTxt = (rem != null) ? rem + " s" : "-";

        holder.textState.setText("Estado: " + estado + "   •   Restante: " + remTxt);

        // Color según estado
        int color;
        String stateLower = estado.toLowerCase();
        if (stateLower.contains("green")) {
            color = holder.itemView.getContext().getColor(R.color.tl_green);
        } else if (stateLower.contains("red")) {
            color = holder.itemView.getContext().getColor(R.color.tl_red);
        } else if (stateLower.contains("amber") || stateLower.contains("yellow")) {
            color = holder.itemView.getContext().getColor(R.color.tl_amber);
        } else {
            color = holder.itemView.getContext().getColor(R.color.tl_text_primary);
        }
        holder.textState.setTextColor(color);

        Integer ciclo = m.getCycleCount();
        String tipo = m.getTrafficLightType() != null ? m.getTrafficLightType() : "-";
        String dir  = m.getCirculationDirection() != null ? m.getCirculationDirection() : "-";

        holder.textExtra.setText(
                "Ciclo: " + (ciclo != null ? ciclo : "-")
                        + "  •  Tipo: " + tipo
                        + "  •  Dir: " + dir
        );
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class MeasurementViewHolder extends RecyclerView.ViewHolder {

        TextView textTimestamp;
        TextView textState;
        TextView textExtra;

        public MeasurementViewHolder(@NonNull View itemView) {
            super(itemView);
            textTimestamp = itemView.findViewById(R.id.textTimestampRow);
            textState     = itemView.findViewById(R.id.textStateRow);
            textExtra     = itemView.findViewById(R.id.textExtraRow);
        }
    }
}
