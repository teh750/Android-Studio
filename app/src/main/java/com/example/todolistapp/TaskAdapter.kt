package com.example.todolistapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(private val tasks: MutableList<Task>, private val listener: OnItemClickListener) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val taskDay: TextView = itemView.findViewById(R.id.taskDay)
        val taskDate: TextView = itemView.findViewById(R.id.taskDate)
        val taskMonth: TextView = itemView.findViewById(R.id.taskMonth)
        val taskYear: TextView = itemView.findViewById(R.id.taskYear)
        val taskTime: TextView = itemView.findViewById(R.id.taskTime)
        val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
        val taskDescription: TextView = itemView.findViewById(R.id.taskDescription)
        val optionsButton: ImageView = itemView.findViewById(R.id.options)

        init {
            itemView.setOnClickListener(this)
            optionsButton.setOnClickListener {
                showPopupMenu(it)
            }
        }

        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }

        private fun showPopupMenu(view: View) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val popupMenu = PopupMenu(view.context, view)
                popupMenu.inflate(R.menu.popup_menu)
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.edit -> {
                            listener.onEditClick(position)
                            true
                        }
                        R.id.delete -> {
                            listener.onDeleteClick(position)
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onEditClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.taskDay.text = currentTask.day
        holder.taskDate.text = currentTask.date
        holder.taskMonth.text = currentTask.month
        holder.taskYear.text = currentTask.year.toString()
        holder.taskTime.text = currentTask.time
        holder.taskTitle.text = currentTask.title
        holder.taskDescription.text = currentTask.description
    }

    override fun getItemCount() = tasks.size
}
