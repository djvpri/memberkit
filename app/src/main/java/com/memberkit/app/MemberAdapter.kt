package com.memberkit.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class MemberAdapter(
    private val onClick: (Member) -> Unit,
    private val onLongClick: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.VH>() {

    private var items: List<Member> = emptyList()
    private val today: LocalDate get() = LocalDate.now()

    fun submit(list: List<Member>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: TextView = v.findViewById(R.id.avatar)
        val name: TextView = v.findViewById(R.id.name)
        val sub: TextView = v.findViewById(R.id.sub)
        val pill: TextView = v.findViewById(R.id.pill)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val ctx = holder.itemView.context
        val st = m.status(today)
        val d = m.daysLeft(today)

        holder.avatar.text = m.initials()
        holder.name.text = m.name
        holder.sub.text = "${m.app} · s/d ${Dates.fmt(m.endDate)}"
        holder.pill.text = when (st) {
            Status.EXPIRED -> "Expired"
            Status.SOON -> if (d == 0L) "Hari ini" else "$d hari"
            Status.ACTIVE -> "$d hari"
        }

        val (bg, fg) = when (st) {
            Status.EXPIRED -> R.color.red_soft to R.color.red
            Status.SOON -> R.color.amber_soft to R.color.amber
            Status.ACTIVE -> R.color.primary_soft to R.color.primary_dark
        }
        holder.pill.setBackgroundResource(R.drawable.bg_pill)
        holder.pill.setBackgroundTintList(ctx.getColorStateList(bg))
        holder.pill.setTextColor(ctx.getColor(fg))

        holder.avatar.setBackgroundResource(R.drawable.bg_avatar)
        holder.avatar.setBackgroundTintList(ctx.getColorStateList(if (st == Status.EXPIRED) R.color.red_soft else if (st == Status.SOON) R.color.amber_soft else R.color.primary_soft))
        holder.avatar.setTextColor(ctx.getColor(if (st == Status.EXPIRED) R.color.red else if (st == Status.SOON) R.color.amber else R.color.primary_dark))

        holder.itemView.setOnClickListener { onClick(m) }
        holder.itemView.setOnLongClickListener { onLongClick(m); true }
    }
}
