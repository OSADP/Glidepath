/**
 * Add provided list of class names to listbox (select)
 *
 * @param consumers
 */
function addConsumersToList(consumers)   {
    "use strict"

    var select = document.getElementById('consumerListId');

    for (var i = 0; i < consumers.length; i++)   {
        var opt = document.createElement('option');
        opt.value = consumers[i];
        opt.innerHTML = consumers[i];;
        select.appendChild(opt);
    }
}


$(document).ready(function() {
    DviAjax.ajaxStringList("listConsumers");
});
