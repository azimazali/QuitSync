import './bootstrap';

import Alpine from 'alpinejs';

Alpine.data('dashboardLogger', () => ({
    showModal: false,
    logType: 'smoked',
    quantity: 1,
    notes: '',
    lat: '',
    lng: '',
    address: '',
    locationStatus: 'Ready to locate.',
    isSubmitting: false,

    openLogModal(type) {
        console.log('Opening modal for:', type);
        this.logType = type;
        this.quantity = 1;
        this.notes = '';
        this.isSubmitting = false;
        this.showModal = true;
        this.getLocation();
    },

    getLocation() {
        this.locationStatus = 'Acquiring location...';
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
                (position) => {
                    this.lat = position.coords.latitude;
                    this.lng = position.coords.longitude;
                    this.locationStatus = 'Location acquired.';

                    // Basic Geocoding
                    if (typeof google !== 'undefined' && google.maps && google.maps.Geocoder) {
                        const geocoder = new google.maps.Geocoder();
                        const latlng = {
                            lat: this.lat,
                            lng: this.lng
                        };
                        geocoder.geocode({
                            location: latlng
                        }, (results, status) => {
                            if (status === 'OK' && results[0]) {
                                this.address = results[0].formatted_address;
                                this.locationStatus = 'Address found: ' + this.address;
                            }
                        });
                    }
                },
                (error) => {
                    console.error('Geolocation error:', error);
                    this.locationStatus = 'Location failed. You can still submit.';
                }
            );
        } else {
            this.locationStatus = 'Geolocation not supported.';
        }
    },

    submitLog() {
        if (this.isSubmitting) return; // Prevent double submit

        this.isSubmitting = true;
        console.log('Submitting log...');
        document.getElementById('dashboardLogForm').submit();
    }
}));

window.Alpine = Alpine;

Alpine.start();
