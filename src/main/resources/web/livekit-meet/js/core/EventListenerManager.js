var EventListenerManager = (function() {
    function create() {
        var listeners = [];
        var intervals = [];
        var timeouts = [];

        function addListener(element, eventType, handler, options) {
            element.addEventListener(eventType, handler, options);
            listeners.push({
                element: element,
                eventType: eventType,
                handler: handler,
                options: options
            });
        }

        function removeListener(element, eventType, handler) {
            element.removeEventListener(eventType, handler);
            listeners = listeners.filter(function(l) {
                return !(l.element === element && l.eventType === eventType && l.handler === handler);
            });
        }

        function addInterval(callback, delay) {
            var id = setInterval(callback, delay);
            intervals.push(id);
            return id;
        }

        function clearIntervalById(id) {
            clearInterval(id);
            intervals = intervals.filter(function(i) { return i !== id; });
        }

        function addTimeout(callback, delay) {
            var id = setTimeout(callback, delay);
            timeouts.push(id);
            return id;
        }

        function clearTimeoutById(id) {
            clearTimeout(id);
            timeouts = timeouts.filter(function(t) { return t !== id; });
        }

        function cleanup() {
            listeners.forEach(function(l) {
                try {
                    l.element.removeEventListener(l.eventType, l.handler, l.options);
                } catch (e) {
                }
            });
            listeners = [];

            intervals.forEach(function(id) {
                clearInterval(id);
            });
            intervals = [];

            timeouts.forEach(function(id) {
                clearTimeout(id);
            });
            timeouts = [];
        }

        function getListenerCount() {
            return listeners.length;
        }

        function getIntervalCount() {
            return intervals.length;
        }

        function getTimeoutCount() {
            return timeouts.length;
        }

        return {
            addListener: addListener,
            removeListener: removeListener,
            addInterval: addInterval,
            clearInterval: clearIntervalById,
            addTimeout: addTimeout,
            clearTimeout: clearTimeoutById,
            cleanup: cleanup,
            getListenerCount: getListenerCount,
            getIntervalCount: getIntervalCount,
            getTimeoutCount: getTimeoutCount
        };
    }

    return {
        create: create
    };
})();

window.EventListenerManager = EventListenerManager;
