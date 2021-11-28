package yapp.android1.delibuddy.ui.address.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import yapp.android1.delibuddy.R
import yapp.android1.delibuddy.base.BaseFragment
import yapp.android1.delibuddy.databinding.FragmentAddressDetailBinding
import yapp.android1.delibuddy.ui.address.AddressSharedViewModel
import yapp.android1.delibuddy.util.extensions.repeatOnStarted

@AndroidEntryPoint
class AddressDetailFragment :
    BaseFragment<FragmentAddressDetailBinding>(FragmentAddressDetailBinding::inflate),
    OnMapReadyCallback {
    private val viewModel: AddressDetailViewModel by viewModels()
    private val addressSharedViewModel: AddressSharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initMap()
        initObserve()
    }

    private fun initView() = with(binding) {
        tvAddressDetailName.text = addressSharedViewModel.selectedAddress.value!!.addressName

        btnAddressDetail.setOnClickListener {
            viewModel.occurEvent(
                AddressDetailEvent.SaveAddress(addressSharedViewModel.selectedAddress.value!!)
            )
        }
    }

    private fun initMap() = with(binding) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                childFragmentManager.beginTransaction().add(R.id.fragment_map, it).commit()
            }
        mapFragment.getMapAsync(this@AddressDetailFragment)
    }

    override fun onMapReady(map: NaverMap) {
        val mapUiSettings = map.uiSettings
        mapUiSettings.isScrollGesturesEnabled = true
        mapUiSettings.isTiltGesturesEnabled = false
        mapUiSettings.isRotateGesturesEnabled = false

        map.cameraPosition = CameraPosition(
            LatLng(
                addressSharedViewModel.selectedAddress.value!!.lat,
                addressSharedViewModel.selectedAddress.value!!.lon
            ),
            16.0
        )

        val marker = Marker()
        marker.position = map.cameraPosition.target
        marker.icon = OverlayImage.fromResource(R.drawable.icon_marker)
        marker.map = map

        map.addOnCameraChangeListener { _, _ ->
            marker.position = map.cameraPosition.target
        }

        map.addOnCameraIdleListener {
            marker.position = map.cameraPosition.target
            Timber.w("lat: ${marker.position.latitude}, lng: ${marker.position.longitude}")
            viewModel.occurEvent(
                AddressDetailEvent.CoordToAddress(
                    marker.position.latitude,
                    marker.position.longitude
                )
            )
        }
    }

    private fun initObserve() = with(binding) {
        repeatOnStarted {
            viewModel.addressResult.collect {
                if (it != null) {
                    tvAddressDetailName.text = it.addressName
                    if (it.roadAddress != "") {
                        tvAddress.text = it.roadAddress
                    } else {
                        tvAddress.text = it.address
                    }
                } else {
                    tvAddressDetailName.text = "다시 시도해 주세요"
                    tvAddress.text = "주소를 찾을 수 없습니다"
                }
            }
        }

        repeatOnStarted {
            viewModel.showToast.collect {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
}