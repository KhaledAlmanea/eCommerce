package com.example.ecommerce.ui.cart;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecommerce.ConfirmFinalOrderActivity;
import com.example.ecommerce.HomeActivity;
import com.example.ecommerce.Model.Cart;
import com.example.ecommerce.Prevalent.Prevalent;
import com.example.ecommerce.R;
import com.example.ecommerce.ui.ProductDetailsActivity;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CartFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private Button NextProcessBtn;
    private TextView txtTotalAmount, txtMsg1;

    private int overTotalPrice = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        recyclerView = root.findViewById(R.id.cart_list);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        NextProcessBtn = (Button) root.findViewById(R.id.next_process_btn);
        txtTotalAmount = (TextView) root.findViewById(R.id.total_price);
        txtMsg1 = (TextView) root.findViewById(R.id.msg1);




        NextProcessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ConfirmFinalOrderActivity.class);
                intent.putExtra("Total Price", String.valueOf(overTotalPrice));
                startActivity(intent);
                getActivity().finish();
            }
        });



        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        CheckOrderState();

        final DatabaseReference cartListRef = FirebaseDatabase.getInstance()
                .getReference().child("Cart List");

        FirebaseRecyclerOptions<Cart> options =
                new FirebaseRecyclerOptions.Builder<Cart>()
                .setQuery(cartListRef.child("User View")
                .child(Prevalent.currentOnlineUsers.getPhone()).child("Products"), Cart.class)
                .build();

        FirebaseRecyclerAdapter<Cart, CartViewHolder> adapter =
                new FirebaseRecyclerAdapter<Cart, CartViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull CartViewHolder holder, int i, @NonNull final Cart model) {

                        holder.txtProductQuantity.setText("Quantity = " + model.getQuantity());
                        holder.txtProductPrice.setText("Price " +model.getPrice() + "$");
                        holder.txtProductName.setText(model.getPname());

                        int oneTypeProductTPrice = ((Integer.parseInt(model.getPrice())))*
                                Integer.parseInt(model.getQuantity());
                        overTotalPrice = overTotalPrice + oneTypeProductTPrice;

                        txtTotalAmount.setText("Total Price = $" + String.valueOf(overTotalPrice));

                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CharSequence options[] = new CharSequence[]
                                        {
                                                "Edit",
                                                "Remove"
                                        };
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle("Cart Options:");

                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        if (i == 0){
                                            Intent intent = new Intent(getActivity(),
                                                    ProductDetailsActivity.class);
                                            intent.putExtra("pid", model.getPid());
                                            startActivity(intent);
                                        }

                                        else if(i == 1){

                                            cartListRef.child("User View")
                                                    .child(Prevalent.currentOnlineUsers.getPhone())
                                                    .child("Products")
                                                    .child(model.getPid())
                                                    .removeValue()
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()){
                                                                Toast.makeText(getActivity(),
                                                                        "Item is removed",
                                                                        Toast.LENGTH_SHORT).show();

                                                                Intent intent = new Intent(getActivity(),
                                                                        HomeActivity.class);
                                                                startActivity(intent);
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                });
                                builder.show();

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.cart_items_layout, parent, false);
                        CartViewHolder holder = new CartViewHolder(view);
                        return holder;
                    }
                };

        recyclerView.setAdapter(adapter);
        adapter.startListening();


    }

    private void CheckOrderState(){

        DatabaseReference ordersRef;
        ordersRef = FirebaseDatabase.getInstance().getReference().child("Orders")
                .child(Prevalent.currentOnlineUsers.getPhone());
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String shippingState = dataSnapshot.child("state").getValue().toString();
                    String userName = dataSnapshot.child("name").getValue().toString();

                    if (shippingState.equals("shipped")){
                        txtTotalAmount.setText("Dear " + userName + "\n order is shipped successfully.");
                        recyclerView.setVisibility(View.GONE);

                        txtMsg1.setText("Congratulations, your final order has been shipped successfully. " +
                                "Soon you will receive your order at your door step.");
                        txtMsg1.setVisibility(View.VISIBLE);
                        NextProcessBtn.setVisibility(View.GONE);

                        Toast.makeText(getActivity(), "You can purchase more product once " +
                                "you received your first final order.", Toast.LENGTH_SHORT).show();
                    }
                    else if (shippingState.equals("not shipped")){
                        txtTotalAmount.setText("Shipping state = Not Shipped");
                        recyclerView.setVisibility(View.GONE);

                        txtMsg1.setVisibility(View.VISIBLE);
                        NextProcessBtn.setVisibility(View.GONE);

                        Toast.makeText(getActivity(), "You can purchase more product once " +
                                "you received your first final order.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
}
