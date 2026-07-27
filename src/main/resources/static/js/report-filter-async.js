(() => {
    const filterForms = '.report-filter-form, .report-period-form';

    const refreshReport = async form => {
        const results = document.getElementById('report-results');
        if (!results || !window.fetch) {
            form.submit();
            return;
        }

        const url = new URL(form.action, window.location.origin);
        url.search = new URLSearchParams(new FormData(form)).toString();
        results.setAttribute('aria-busy', 'true');

        try {
            const response = await fetch(url, {
                credentials: 'same-origin',
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });
            if (!response.ok) throw new Error('Unable to refresh report');

            const documentResponse = new DOMParser().parseFromString(await response.text(), 'text/html');
            const nextResults = documentResponse.getElementById('report-results');
            if (!nextResults) throw new Error('Report results were not returned');

            results.innerHTML = nextResults.innerHTML;

            const nextPreview = documentResponse.querySelector('.report-pdf-preview');
            const previewButton = document.querySelector('.report-pdf-preview');
            if (previewButton && nextPreview?.dataset.pdfUrl) {
                previewButton.dataset.pdfUrl = nextPreview.dataset.pdfUrl;
            }

            window.history.replaceState(null, '', window.location.pathname);
            window.hotelInitTablePagination?.(results);
            window.hotelInitSortableTables?.(results);
        } catch (error) {
            if (window.Swal) {
                window.Swal.fire({ icon: 'error', title: 'ไม่สามารถโหลดรายงานได้' });
            }
        } finally {
            results.removeAttribute('aria-busy');
        }
    };

    if (document.querySelector(filterForms) && window.location.search) {
        window.history.replaceState(null, '', window.location.pathname);
    }

    document.querySelectorAll(filterForms).forEach(form => {
        form.addEventListener('submit', event => {
            event.preventDefault();
            refreshReport(form);
        });
    });
})();
