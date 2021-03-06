package org.fossasia.openevent.general.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_order_details.view.orderDetailsRecycler
import kotlinx.android.synthetic.main.fragment_order_details.view.progressBar
import org.fossasia.openevent.general.MainActivity
import org.fossasia.openevent.general.R
import org.fossasia.openevent.general.event.EventDetailsFragment
import org.fossasia.openevent.general.ticket.EVENT_ID
import org.fossasia.openevent.general.utils.extensions.nonNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class OrderDetailsFragment : Fragment() {

    private lateinit var rootView: View
    private var id: Long = -1
    private lateinit var orderId: String
    private val orderDetailsViewModel by viewModel<OrderDetailsViewModel>()
    private val ordersRecyclerAdapter: OrderDetailsRecyclerAdapter = OrderDetailsRecyclerAdapter()
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        if (bundle != null) {
            id = bundle.getLong(EVENT_ID, -1)
            val orderId = bundle.getString(ORDERS)

            if (orderId == null) {
                Timber.e("Order ID must be provided")
            } else {
                this.orderId = orderId
            }
        }
        ordersRecyclerAdapter.setOrderIdentifier(orderId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_order_details, container, false)
        val activity = activity as? AppCompatActivity
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.supportActionBar?.title = ""
        setHasOptionsMenu(true)

        rootView.orderDetailsRecycler.layoutManager = LinearLayoutManager(activity)
        rootView.orderDetailsRecycler.adapter = ordersRecyclerAdapter
        rootView.orderDetailsRecycler.isNestedScrollingEnabled = false

        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rootView.orderDetailsRecycler.layoutManager = linearLayoutManager

        val eventDetailsListener = object : OrderDetailsRecyclerAdapter.EventDetailsListener {
            override fun onClick(eventID: Long) {
                val fragment = EventDetailsFragment()
                val bundle = Bundle()
                bundle.putLong(EVENT_ID, eventID)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.rootLayout, fragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }
        }

        ordersRecyclerAdapter.setListener(eventDetailsListener)
        orderDetailsViewModel.event
            .nonNull()
            .observe(this, Observer {
                ordersRecyclerAdapter.setEvent(it)
                ordersRecyclerAdapter.notifyDataSetChanged()
            })

        orderDetailsViewModel.progress
            .nonNull()
            .observe(this, Observer {
                rootView.progressBar.isVisible = it
            })

        orderDetailsViewModel.attendees
            .nonNull()
            .observe(this, Observer {
                ordersRecyclerAdapter.addAll(it)
                ordersRecyclerAdapter.notifyDataSetChanged()
                Timber.d("Fetched attendees of size %s", ordersRecyclerAdapter.itemCount)
            })

        orderDetailsViewModel.message
            .nonNull()
            .observe(this, Observer {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            })

        orderDetailsViewModel.loadEvent(id)
        orderDetailsViewModel.loadAttendeeDetails(orderId)

        return rootView
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        val activity = activity as? MainActivity
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        activity?.supportActionBar?.title = "Tickets"
        setHasOptionsMenu(false)
        super.onDestroyView()
    }
}
