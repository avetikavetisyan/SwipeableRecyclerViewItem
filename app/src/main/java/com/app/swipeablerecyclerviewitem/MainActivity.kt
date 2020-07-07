package com.app.swipeablerecyclerviewitem

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.swipeableitem.SwipeableItem
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.background_view.view.*
import kotlinx.android.synthetic.main.foreground_view.view.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.setHasFixedSize(true)

        val list = listOf("a", "b", "c", "d", "e", "f", "g", "h", "j", "k", "l", "m", "n", "o", "p", "q", "v", "w", "x", "y", "z")

        recycler_view.adapter = MyAdapter(list)
    }

    class MyAdapter(private val list: List<String>) :RecyclerView.Adapter<MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_item, parent, false) as SwipeableItem

            val holder = MyViewHolder(view)

            view.phone.setOnClickListener { v ->
                Snackbar.make(v, "Phone: field ${holder.view.text.text}", Snackbar.LENGTH_LONG).show()
            }
            view.message.setOnClickListener { v ->
                Snackbar.make(v, "Message: field ${holder.view.text.text}", Snackbar.LENGTH_LONG).show()
            }
            view.delete.setOnClickListener { v ->
                Snackbar.make(v, "Delete: field ${holder.view.text.text}", Snackbar.LENGTH_LONG).show()
            }

            return holder
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.view.onBindViewHolder(position)
            holder.view.text.text = list[position]
        }

        override fun getItemCount() = list.size

    }

    class MyViewHolder(val view: SwipeableItem) : RecyclerView.ViewHolder(view)
}