package c.chasesriprajittichai.stockwatch;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;


public class RecyclerHomeAdapter extends RecyclerView.Adapter<RecyclerHomeAdapter.ViewHolder> {


    public interface OnItemClickListener {
        void onItemClick(BasicStock basicStock);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tickerTextView;
        private TextView priceTextView;
        private TextView priceChangePercentTextView;


        public ViewHolder(View v) {
            super(v);

            tickerTextView = v.findViewById(R.id.textView_ticker_homeRecyclerItem);
            priceTextView = v.findViewById(R.id.textView_price_homeRecyclerItem);
            priceChangePercentTextView = v.findViewById(R.id.textView_priceChangePercent_homeRecyclerItem);
        }


        public void bind(final BasicStock basicStock, final OnItemClickListener listener) {
            tickerTextView.setText(basicStock.getTicker());
            priceTextView.setText(String.format(Locale.US, "%.2f", basicStock.getPrice()));
            // Append '%' onto end of price change percent. First '%' is escape char for '%'.
            priceChangePercentTextView.setText(String.format(Locale.US, "%.2f%%", basicStock.getChangePercent()));

            // Assign green or red color to price change percent text
            if (basicStock.getChangePercent() < 0) {
                priceChangePercentTextView.setTextColor(Color.RED);
            } else {
                priceChangePercentTextView.setTextColor(Color.GREEN);
            }

            itemView.setOnClickListener(l -> listener.onItemClick(basicStock));
        }
    }


    private ArrayList<BasicStock> basicStocks;
    private OnItemClickListener onItemClickListener;


    public RecyclerHomeAdapter(ArrayList<BasicStock> basicStocks, OnItemClickListener listener) {
        this.basicStocks = basicStocks;
        this.onItemClickListener = listener;
    }


    @NonNull
    @Override
    public RecyclerHomeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View stockView = inflater.inflate(R.layout.recycler_item_home_activity, parent, false);

        return new ViewHolder(stockView);
    }


    @NonNull
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(basicStocks.get(position), onItemClickListener);
    }


    @Override
    public int getItemCount() {
        return basicStocks.size();
    }

}