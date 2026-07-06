(function () {
    const alertConfig = {
        create: { className: 'hotel-toast-create', icon: 'bi-check-circle' },
        success: { className: 'hotel-toast-success', icon: 'bi-check-circle' },
        update: { className: 'hotel-toast-update', icon: 'bi-pencil-square' },
        edit: { className: 'hotel-toast-edit', icon: 'bi-pencil-square' },
        delete: { className: 'hotel-toast-delete', icon: 'bi-trash3' },
        danger: { className: 'hotel-toast-danger', icon: 'bi-trash3' },
        warning: { className: 'hotel-toast-warning', icon: 'bi-exclamation-triangle' }
    };
    let alertSequence = 0;

    const dismissToast = (toast) => {
        if (!toast || !toast.isConnected || toast.classList.contains('is-hiding')) return;
        toast.classList.add('is-hiding');
        window.setTimeout(() => toast.remove(), 900);
    };

    window.dismissAlert = (id) => dismissToast(document.getElementById(id));
    window.showAlert = (type, message, duration = 3000) => {
        const container = document.getElementById('hotelToastContainer');
        if (!container || !message) return null;

        const config = alertConfig[type] || alertConfig.warning;
        const id = `hotel-toast-${Date.now()}-${++alertSequence}`;
        const toast = document.createElement('div');
        toast.id = id;
        toast.className = `hotel-toast-item ${config.className}`;
        toast.setAttribute('role', 'alert');
        toast.setAttribute('aria-live', 'polite');
        toast.innerHTML = `
            <i class="bi ${config.icon}" aria-hidden="true"></i>
            <span class="hotel-toast-message"></span>
            <button class="hotel-toast-close" type="button" aria-label="ปิดการแจ้งเตือน">&times;</button>
        `;
        toast.querySelector('.hotel-toast-message').textContent = message;
        container.prepend(toast);

        const dismiss = () => dismissToast(toast);
        toast.querySelector('.hotel-toast-close').addEventListener('click', dismiss);
        window.setTimeout(dismiss, Number(duration) || 3000);
        return id;
    };

    const serverFlashToast = document.getElementById('serverFlashToast');
    if (serverFlashToast && serverFlashToast.dataset.shown !== 'true') {
        serverFlashToast.dataset.shown = 'true';
        window.showAlert(serverFlashToast.dataset.type, serverFlashToast.dataset.message);
    }
})();
