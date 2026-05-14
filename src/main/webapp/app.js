(function () {
    var form = document.getElementById('traceForm');
    var output = document.getElementById('output');
    var health = document.getElementById('health');
    var httpStatus = document.getElementById('httpStatus');
    var elapsed = document.getElementById('elapsed');
    var query = document.getElementById('query');
    var trace = document.getElementById('trace');
    var runButton = document.getElementById('runButton');
    var methodBadge = document.getElementById('methodBadge');
    var lastRequest = document.getElementById('lastRequest');
    var operations = document.querySelectorAll('.operation');
    var queueRows = document.querySelectorAll('tbody tr');
    var selectedMethod = 'GET';
    var selectedOperation = 'retrieve-workspace';
    var selectedPath = 'workspace';
    var selectedLabel = '查詢訂單工作區';

    function apiBase() {
        var path = window.location.pathname;
        var nextSlash = path.indexOf('/', 1);
        var context = nextSlash > -1 ? path.substring(0, nextSlash) : '';
        return context + '/api';
    }

    function setMetric(responseStatus, body) {
        httpStatus.textContent = String(responseStatus);
        elapsed.textContent = body.elapsedMillis ? body.elapsedMillis + ' ms' : '-';
        query.textContent = body.queryMillis ? body.queryMillis + ' ms' : '-';
        trace.textContent = body.traceActive ? '啟用' : '未啟用';
    }

    function render(responseStatus, body) {
        setMetric(responseStatus, body);
        output.textContent = JSON.stringify(body, null, 2);
    }

    function setOperation(button) {
        for (var i = 0; i < operations.length; i++) {
            operations[i].className = operations[i].className.replace(' active', '');
        }
        button.className += ' active';
        selectedMethod = button.getAttribute('data-method');
        selectedOperation = button.getAttribute('data-operation');
        selectedPath = button.getAttribute('data-path');
        selectedLabel = button.getAttribute('data-label');
        methodBadge.textContent = selectedMethod;
        runButton.textContent = '執行 ' + selectedMethod + '：' + selectedLabel;
    }

    function checkHealth() {
        fetch(apiBase() + '/health')
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('health check failed');
                }
                health.textContent = '正常';
                health.className = 'status ok';
            })
            .catch(function () {
                health.textContent = '異常';
                health.className = 'status error';
            });
    }

    for (var i = 0; i < operations.length; i++) {
        operations[i].addEventListener('click', function () {
            setOperation(this);
        });
    }

    for (var j = 0; j < queueRows.length; j++) {
        queueRows[j].addEventListener('click', function () {
            document.getElementById('orderId').value = this.getAttribute('data-order');
            document.getElementById('customerId').value = this.getAttribute('data-customer');
        });
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var orderIdValue = document.getElementById('orderId').value;
        var customerIdValue = document.getElementById('customerId').value;
        var orderId = encodeURIComponent(orderIdValue);
        var customerId = encodeURIComponent(customerIdValue);
        var scenario = encodeURIComponent(document.getElementById('scenario').value);
        var url = apiBase() + '/orders/' + orderId + '/' + selectedPath + '?customerId=' + customerId + '&scenario=' + scenario;

        runButton.disabled = true;
        lastRequest.textContent = selectedMethod + ' /orders/' + orderIdValue + '/' + selectedPath;
        fetch(url, {method: selectedMethod})
            .then(function (response) {
                return response.json().then(function (body) {
                    render(response.status, body);
                });
            })
            .catch(function (error) {
                render('ERR', {status: 'ERROR', message: error.message});
            })
            .then(function () {
                runButton.disabled = false;
            });
    });

    checkHealth();
    runButton.textContent = '執行 ' + selectedMethod + '：' + selectedLabel;
}());
