(() => {
    const initialisePreview = () => {
        const previewButton = document.querySelector('.report-pdf-preview');
        if (!previewButton || !previewButton.dataset.pdfUrl || previewButton.dataset.previewReady === 'true') return;
        previewButton.dataset.previewReady = 'true';

        let objectUrl = null;
        let modalElement = document.getElementById('reportPdfPreviewModal');
        if (!modalElement) {
            modalElement = document.createElement('div');
            modalElement.id = 'reportPdfPreviewModal';
            modalElement.className = 'modal fade';
            modalElement.tabIndex = -1;
            modalElement.innerHTML = `
                <div class="modal-dialog modal-xl modal-dialog-centered" style="height:90vh">
                    <div class="modal-content" style="height:100%;overflow:hidden">
                        <div class="modal-header"><h5 class="modal-title">\u0e14\u0e39\u0e15\u0e31\u0e27\u0e2d\u0e22\u0e48\u0e32\u0e07\u0e23\u0e32\u0e22\u0e07\u0e32\u0e19</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div>
                        <div class="modal-body p-0" style="flex:1 1 auto;min-height:0;overflow:hidden"><iframe class="report-pdf-frame" title="\u0e15\u0e31\u0e27\u0e2d\u0e22\u0e48\u0e32\u0e07\u0e23\u0e32\u0e22\u0e07\u0e32\u0e19 PDF" scrolling="no" style="display:block;width:100%;height:100%;border:0;overflow:hidden"></iframe></div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">\u0e1b\u0e34\u0e14</button>
                            <button type="button" class="btn btn-primary report-pdf-save"><i class="bi bi-download me-1"></i>\u0e1a\u0e31\u0e19\u0e17\u0e36\u0e01\u0e44\u0e1f\u0e25\u0e4c</button>
                            <button type="button" class="btn btn-light-primary report-pdf-print"><i class="bi bi-printer me-1"></i>\u0e1e\u0e34\u0e21\u0e1e\u0e4c</button>
                        </div>
                    </div>
                </div>`;
            document.body.appendChild(modalElement);
        }

        const Modal = window.bootstrap?.Modal;
        if (!Modal) return;
        const modal = typeof Modal.getOrCreateInstance === 'function'
            ? Modal.getOrCreateInstance(modalElement)
            : (typeof Modal.getInstance === 'function' && Modal.getInstance(modalElement)) || new Modal(modalElement);
        const frame = modalElement.querySelector('.report-pdf-frame');
        const saveButton = modalElement.querySelector('.report-pdf-save');
        const printButton = modalElement.querySelector('.report-pdf-print');
        const clearPreview = () => {
            frame.src = 'about:blank';
            if (objectUrl) URL.revokeObjectURL(objectUrl);
            objectUrl = null;
        };

        modalElement.addEventListener('hidden.bs.modal', clearPreview);
        saveButton.addEventListener('click', () => {
            if (!objectUrl) return;
            const anchor = document.createElement('a');
            anchor.href = objectUrl;
            anchor.download = 'report.pdf';
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
        });
        printButton.addEventListener('click', () => frame.contentWindow?.print());

        previewButton.addEventListener('click', async () => {
            const pdfUrl = previewButton.dataset.pdfUrl;
            if (!pdfUrl) return;
            const previewUrl = new URL(pdfUrl, window.location.origin);
            previewUrl.searchParams.set('inline', 'true');
            previewButton.disabled = true;
            window.hotelLoading?.start();
            try {
                const response = await fetch(previewUrl, { credentials: 'same-origin' });
                if (!response.ok) throw new Error('Unable to generate report PDF');
                const pdf = await response.blob();
                clearPreview();
                objectUrl = URL.createObjectURL(pdf);
                frame.addEventListener('load', () => {
                    previewButton.disabled = false;
                    window.hotelLoading?.finish();
                }, { once: true });
                frame.src = objectUrl;
                modal?.show();
            } catch (error) {
                previewButton.disabled = false;
                window.hotelLoading?.finish();
                if (window.Swal) {
                    window.Swal.fire({ icon: 'error', title: '\u0e44\u0e21\u0e48\u0e2a\u0e32\u0e21\u0e32\u0e23\u0e16\u0e2a\u0e23\u0e49\u0e32\u0e07\u0e15\u0e31\u0e27\u0e2d\u0e22\u0e48\u0e32\u0e07\u0e23\u0e32\u0e22\u0e07\u0e32\u0e19\u0e44\u0e14\u0e49' });
                }
            }
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialisePreview);
    } else {
        initialisePreview();
    }
})();
