package adapters;

import entities.CartProduct;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.widget.Toast;

import com.example.navigator.Product;
import com.example.navigator.R;
import com.example.navigator.utils.DatabaseConn;
import com.example.navigator.utils.Installation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import static com.example.navigator.R.layout.cart_product_list_layout;



public class CartProductListAdapter extends ArrayAdapter<CartProduct> {
    private Context context;
    private List<CartProduct> products;


    //Get device ID
    final String deviceId = Installation.id(getContext());

    DatabaseReference cartDBRef = FirebaseDatabase.getInstance().getReference().child("Cart").child(deviceId);
    DatabaseReference wishDBRef = FirebaseDatabase.getInstance().getReference().child("Wishlist").child(deviceId);



    public CartProductListAdapter(Context context, List<CartProduct> products) {
        super(context, R.layout.cart_product_list_layout, products);
        this.context = context;
        this.products = products;
    }

    @NonNull
    //@Override
    public View getView(final int position, View view, ViewGroup parent) {

        final ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(cart_product_list_layout, parent, false);
            viewHolder.textViewName = view.findViewById(R.id.textViewName);
            viewHolder.textViewPrice = view.findViewById(R.id.textViewPrice);
            viewHolder.textViewQuantity = view.findViewById(R.id.textQuantity);
            viewHolder.imageViewPhoto = view.findViewById(R.id.imageViewPhoto);
            viewHolder.totalPrice = view.findViewById(R.id.totalPrice);
            viewHolder.incrementQuantity = view.findViewById(R.id.incrementQuantity);
            viewHolder.decrementQuantity = view.findViewById(R.id.decrementQuantity);
            viewHolder.deleteCartProduct = view.findViewById(R.id.deleteCartItem);
            viewHolder.addToWishList = view.findViewById(R.id.addToWishlist);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        final CartProduct product = products.get(position);
        viewHolder.textViewName.setText(product.getName());
        viewHolder.textViewQuantity.setText(product.getQuantity());
        viewHolder.textViewPrice.setText("R " + product.getPrice());
        //viewHolder.imageViewPhoto.setImageResource(product.getPhoto());
        viewHolder.totalPrice.setText(product.getTotalPrice());

        viewHolder.imageViewPhoto.setImageBitmap(product.getBmap());

        CartProduct currCartProduct = new CartProduct();

        try{
            final File localFile = File.createTempFile("images","jpg");
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference imageRef = storage.getReferenceFromUrl("gs://bruteforce-d8058.appspot.com").child(product.getId()+ ".jpg");



            imageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    viewHolder.imageViewPhoto.setImageBitmap(bitmap);
                    //Toast.makeText(getContext(),"Local File name: " + localFile.getName() + " image name "+ product.getImageName() , Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }


        //Increasing Quantity through button
        viewHolder.incrementQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Updated quantity on display
                viewHolder.textViewQuantity.setText(product.increaseQuantity());
                viewHolder.totalPrice.setText(product.getTotalPrice());
                //Query to find the ID
                Query myQuery = cartDBRef.orderByChild("id").equalTo(product.getId());


                ValueEventListener valueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot dataSnap : dataSnapshot.getChildren())
                        {
                            dataSnap.child("quantity").getRef().setValue(product.getQuantity());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };
                myQuery.addListenerForSingleValueEvent(valueEventListener);


            }
        });

        //Decrementing the quality
        viewHolder.decrementQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Updated quantity on display
                viewHolder.textViewQuantity.setText(product.decreaseQuantity());
                viewHolder.totalPrice.setText(product.getTotalPrice());
                //Query to find the ID
                Query myQuery = cartDBRef.orderByChild("id").equalTo(product.getId());


                ValueEventListener valueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot dataSnap : dataSnapshot.getChildren())
                        {
                            dataSnap.child("quantity").getRef().setValue(product.getQuantity());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };
                myQuery.addListenerForSingleValueEvent(valueEventListener);

                notifyDataSetChanged();


            }
        });


        final CartProduct currProduct = products.get(position);

        viewHolder.deleteCartProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Query myQuery = cartDBRef.orderByChild("id").equalTo(product.getId());

                myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for(DataSnapshot dataSnap : dataSnapshot.getChildren())
                        {
                            String toDelete = dataSnap.getKey();
                            DatabaseConn data = DatabaseConn.open();
                            Toast.makeText(getContext(),product.getName()+ " removed from Cart ", Toast.LENGTH_LONG).show();
                            data.delete("Cart",deviceId+"/"+toDelete);
                        }

                        removeFromList(currProduct);

                    }



                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });





            }
        });

        viewHolder.addToWishList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wishDBRef.push().setValue(product);
                Toast.makeText(getContext(),product.getName()+ " added to WishList ", Toast.LENGTH_LONG).show();

                Query myQuery = cartDBRef.orderByChild("id").equalTo(product.getId());

                myQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for(DataSnapshot dataSnap : dataSnapshot.getChildren())
                        {
                            String toDelete = dataSnap.getKey();
                            DatabaseConn data = DatabaseConn.open();

                            data.delete("Cart",deviceId+"/"+toDelete);
                        }

                        removeFromList(currProduct);

                    }



                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });




        return view;
    }

    private void removeFromList(CartProduct cp) {
        int position = products.indexOf(cp);
        products.remove(position);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public static TextView textViewName;
        public static TextView textViewQuantity;
        public static TextView textViewPrice;
        public static ImageView imageViewPhoto;
        public static TextView totalPrice;
        public static Button incrementQuantity;
        public static Button decrementQuantity;
        public static Button deleteCartProduct;
        public static Button addToWishList;
    }
}
