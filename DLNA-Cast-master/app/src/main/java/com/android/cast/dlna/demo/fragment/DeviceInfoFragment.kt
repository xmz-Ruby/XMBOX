package com.android.cast.dlna.demo.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.cast.dlna.demo.DetailContainer
import com.android.cast.dlna.demo.R
import com.android.cast.dlna.demo.R.layout
import com.android.cast.dlna.dmc.DLNACastManager
import com.android.cast.dlna.dmc.control.DeviceControl
import com.android.cast.dlna.dmc.control.OnDeviceControlListener
import com.android.cast.dlna.dmc.control.ServiceActionCallback
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.support.model.MediaInfo
import org.fourthline.cling.support.model.TransportInfo

class DeviceInfoFragment : Fragment() {

    private val device: Device<*, *, *> by lazy { (requireParentFragment() as DetailContainer).getDevice() }
    private val mediaInfo: TextView by lazy { requireView().findViewById(R.id.info_device_media) }
    private val transportInfo: TextView by lazy { requireView().findViewById(R.id.info_device_transport) }
    private val volume: TextView by lazy { requireView().findViewById(R.id.info_device_volume) }
    private val deviceControl: DeviceControl by lazy { DLNACastManager.connectDevice(device, object : OnDeviceControlListener {}) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.info_get_volume).setOnClickListener {
            deviceControl.getVolume(object : ServiceActionCallback<Int> {
                @SuppressLint("SetTextI18n")
                override fun onSuccess(result: Int) {
                    volume.text = "音量: $result\n"
                }

                override fun onFailure(msg: String) {
                    volume.text = msg
                }
            })
        }
        view.findViewById<View>(R.id.info_get_media).setOnClickListener {
            deviceControl.getMediaInfo(object : ServiceActionCallback<MediaInfo> {
                @SuppressLint("SetTextI18n")
                override fun onSuccess(result: MediaInfo) {
                    mediaInfo.text = "currentURI:\n  ${result.currentURI}\n\n" +
                            "nextURI:\n  ${result.nextURI}"
                }

                override fun onFailure(msg: String) {
                    mediaInfo.text = msg
                }
            })
        }
        view.findViewById<View>(R.id.info_get_transport).setOnClickListener {
            deviceControl.getTransportInfo(object : ServiceActionCallback<TransportInfo> {
                @SuppressLint("SetTextI18n")
                override fun onSuccess(result: TransportInfo) {
                    transportInfo.text = "currentTransportState: ${result.currentTransportState}\n" +
                            "currentTransportStatus: ${result.currentTransportStatus}\n" +
                            "currentSpeed: ${result.currentSpeed}\n"
                }

                override fun onFailure(msg: String) {
                    transportInfo.text = msg
                }
            })
        }
    }
}