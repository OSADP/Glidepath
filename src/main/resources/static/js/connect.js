
var stompClient = null;
var lastLight = "GREEN";
var bConnected = false;

function setConnected(connected) {
    bConnected = connected;
    }

function consume()   {
    if (document.getElementById('btnConsumeId').innerHTML === 'Consume')   {
        DviAjax.ajaxStartStop('/initialize', 'consume');
        document.getElementById('btnConsumeId').innerHTML = 'Stop Consuption';
    }
    else   {
        DviAjax.ajaxStartStop('/terminate', 'consume');
        document.getElementById('btnConsumeId').innerHTML = 'Consume';
    }

}

function connect() {
    var socket = new SockJS('/dvi');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/dvitopic', function(uiMessageResponse){
            var uiMessageObject = JSON.parse(uiMessageResponse.body);
            displayUiMessage(uiMessageObject);
        });
});

}

function disconnect() {
    stompClient.disconnect();
    setConnected(false);
    console.log("Disconnected");

}


