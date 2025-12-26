var EventHandlerRegistry = (function() {
    function create() {
        var handlers = [];
        var roomRef = null;

        function setRoom(room) {
            roomRef = room;
        }

        function register(eventType, handler, context) {
            handlers.push({
                eventType: eventType,
                handler: handler,
                context: context || null
            });
        }

        function attachAll() {
            if (!roomRef) {
                console.error('EventHandlerRegistry: No room set');
                return;
            }
            handlers.forEach(function(h) {
                var boundHandler = h.context ? h.handler.bind(h.context) : h.handler;
                roomRef.on(h.eventType, boundHandler);
            });
        }

        function detachAll() {
            if (!roomRef) return;
            handlers.forEach(function(h) {
                try {
                    roomRef.off(h.eventType, h.handler);
                } catch (e) {
                }
            });
        }

        function clear() {
            detachAll();
            handlers = [];
            roomRef = null;
        }

        function getHandlerCount() {
            return handlers.length;
        }

        return {
            setRoom: setRoom,
            register: register,
            attachAll: attachAll,
            detachAll: detachAll,
            clear: clear,
            getHandlerCount: getHandlerCount
        };
    }

    return {
        create: create
    };
})();

window.EventHandlerRegistry = EventHandlerRegistry;
