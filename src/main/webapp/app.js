(function () {
    var form = document.getElementById('traceForm');
    var output = document.getElementById('output');
    var health = document.getElementById('health');
    var httpStatus = document.getElementById('httpStatus');
    var elapsed = document.getElementById('elapsed');
    var query = document.getElementById('query');
    var trace = document.getElementById('trace');

    function setMetric(responseStatus, body) {
        httpStatus.textContent = String(responseStatus);
        elapsed.textContent = body.elapsedMillis ? body.elapsedMillis + ' ms' : '-';
        query.textContent = body.queryMillis ? body.queryMillis + ' ms' : '-';
        trace.textContent = body.traceActive ? 'active' : 'inactive';
    }

    function render(responseStatus, body) {
        setMetric(responseStatus, body);
        output.textContent = JSON.stringify(body, null, 2);
    }

    function apiBase() {
        var path = window.location.pathname;
        var context = path.substring(0, path.indexOf('/', 1));
        return context + '/api';
    }

    function checkHealth() {
        fetch(apiBase() + '/health')
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('health check failed');
                }
                health.textContent = 'UP';
                health.className = 'status ok';
            })
            .catch(function () {
                health.textContent = 'DOWN';
                health.className = 'status error';
            });
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var button = form.querySelector('button');
        var orderId = encodeURIComponent(document.getElementById('orderId').value);
        var customerId = encodeURIComponent(document.getElementById('customerId').value);
        var scenario = encodeURIComponent(document.getElementById('scenario').value);
        var url = apiBase() + '/orders/' + orderId + '?customerId=' + customerId + '&scenario=' + scenario;

        button.disabled = true;
        fetch(url)
            .then(function (response) {
                return response.json().then(function (body) {
                    render(response.status, body);
                });
            })
            .catch(function (error) {
                render('ERR', {status: 'ERROR', message: error.message});
            })
            .then(function () {
                button.disabled = false;
            });
    });

    checkHealth();
}());
