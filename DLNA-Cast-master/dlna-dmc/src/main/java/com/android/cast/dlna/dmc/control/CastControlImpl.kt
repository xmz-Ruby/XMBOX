package com.android.cast.dlna.dmc.control

import com.android.cast.dlna.dmc.DLNACastManager
import com.android.cast.dlna.dmc.control.BaseServiceExecutor.AVServiceExecutorImpl
import com.android.cast.dlna.dmc.control.BaseServiceExecutor.ContentServiceExecutorImpl
import com.android.cast.dlna.dmc.control.BaseServiceExecutor.RendererServiceExecutorImpl
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser
import org.fourthline.cling.support.lastchange.EventedValue
import org.fourthline.cling.support.model.BrowseFlag
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.MediaInfo
import org.fourthline.cling.support.model.PositionInfo
import org.fourthline.cling.support.model.TransportInfo
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser

class CastControlImpl(
    controlPoint: ControlPoint,
    device: Device<*, *, *>,
    listener: OnDeviceControlListener,
) : DeviceControl {

    private val avTransportService: AVServiceExecutorImpl
    private val renderService: RendererServiceExecutorImpl
    private val contentService: ContentServiceExecutorImpl
    var released = false

    init {
        avTransportService = AVServiceExecutorImpl(controlPoint, device.findService(DLNACastManager.SERVICE_TYPE_AV_TRANSPORT))
        avTransportService.subscribe(object : SubscriptionListener {
            override fun failed(subscriptionId: String?) {
                if (!released) listener.onDisconnected(device)
            }

            override fun established(subscriptionId: String?) {
                if (!released) listener.onConnected(device)
            }

            override fun ended(subscriptionId: String?) {
                if (!released) listener.onDisconnected(device)
            }

            override fun onReceived(subscriptionId: String?, event: EventedValue<*>) {
                if (!released) listener.onEventChanged(event)
            }
        }, AVTransportLastChangeParser())
        renderService = RendererServiceExecutorImpl(controlPoint, device.findService(DLNACastManager.SERVICE_TYPE_RENDERING_CONTROL))
        renderService.subscribe(object : SubscriptionListener {}, RenderingControlLastChangeParser())
        contentService = ContentServiceExecutorImpl(controlPoint, device.findService(DLNACastManager.SERVICE_TYPE_CONTENT_DIRECTORY))
        //TODO: check the parser
        contentService.subscribe(object : SubscriptionListener {}, AVTransportLastChangeParser())
    }

    // --------------------------------------------------------
    // ---- AvTransport ---------------------------------------
    // --------------------------------------------------------
    override fun setAVTransportURI(uri: String, title: String, callback: ServiceActionCallback<Unit>?) {
        avTransportService.setAVTransportURI(uri, title, callback)
    }

    override fun setNextAVTransportURI(uri: String, title: String, callback: ServiceActionCallback<Unit>?) {
        avTransportService.setNextAVTransportURI(uri, title, callback)
    }

    override fun play(speed: String, callback: ServiceActionCallback<Unit>?) {
        avTransportService.play(speed, callback)
    }

    override fun pause(callback: ServiceActionCallback<Unit>?) {
        avTransportService.pause(callback)
    }

    override fun seek(millSeconds: Long, callback: ServiceActionCallback<Unit>?) {
        avTransportService.seek(millSeconds, callback)
    }

    override fun stop(callback: ServiceActionCallback<Unit>?) {
        avTransportService.stop(callback)
    }

    override fun next(callback: ServiceActionCallback<Unit>?) {
        avTransportService.next(callback)
    }

    override fun canNext(callback: ServiceActionCallback<Boolean>?) {
        avTransportService.canNext(callback)
    }

    override fun previous(callback: ServiceActionCallback<Unit>?) {
        avTransportService.previous(callback)
    }

    override fun canPrevious(callback: ServiceActionCallback<Boolean>?) {
        avTransportService.canPrevious(callback)
    }

    override fun getMediaInfo(callback: ServiceActionCallback<MediaInfo>?) {
        avTransportService.getMediaInfo(callback)
    }

    override fun getPositionInfo(callback: ServiceActionCallback<PositionInfo>?) {
        avTransportService.getPositionInfo(callback)
    }

    override fun getTransportInfo(callback: ServiceActionCallback<TransportInfo>?) {
        avTransportService.getTransportInfo(callback)
    }

    // --------------------------------------------------------
    // ---- Renderer ------------------------------------------
    // --------------------------------------------------------
    override fun setVolume(volume: Int, callback: ServiceActionCallback<Unit>?) {
        renderService.setVolume(volume, callback)
    }

    override fun getVolume(callback: ServiceActionCallback<Int>?) {
        renderService.getVolume(callback)
    }

    override fun setMute(mute: Boolean, callback: ServiceActionCallback<Unit>?) {
        renderService.setMute(mute, callback)
    }

    override fun getMute(callback: ServiceActionCallback<Boolean>?) {
        renderService.getMute(callback)
    }

    // --------------------------------------------------------
    // ---- Content -------------------------------------------
    // --------------------------------------------------------
    override fun browse(objectId: String, flag: BrowseFlag, filter: String, firstResult: Int, maxResults: Int, callback: ServiceActionCallback<DIDLContent>?) {
        contentService.browse(objectId, flag, filter, firstResult, maxResults, callback)
    }

    override fun search(containerId: String, searchCriteria: String, filter: String, firstResult: Int, maxResults: Int, callback: ServiceActionCallback<DIDLContent>?) {
        contentService.search(containerId, searchCriteria, filter, firstResult, maxResults, callback)
    }
}