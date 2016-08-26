var dataTablesServerSettings = {
    init: function(tableID, length, sort, bStateSave, fnDrawCallback) {
        this.tableID = tableID;
        
        var options = {
            "sDom": "<'row'<'col-md-6'l><'col-md-6 text-right'f>r>t<'row'<'col-md-6 text-muted'i><'col-md-6 text-right'p>>",
            "iDisplayLength": length,
            "sPaginationType": "bootstrap",
            "aaSorting": sort //this option disable sort by default, the user steal can use column names to sort the table
        };

        if (fnDrawCallback != null) {
            options.fnDrawCallback = fnDrawCallback
        }

        if (bStateSave != null) {
            options.bStateSave = bStateSave
        }

        $('#' + this.tableID).dataTable(options);

        this.bootstrap3LookAndFeel();
    },
    bootstrap3LookAndFeel: function() {
        // fix select and search input
        $('select[name=' + this.tableID + '_length]').addClass('form-control');
        $('#' + this.tableID + '_filter input').addClass('form-control');
        $('#' + this.tableID + '_length').addClass('form-group form-group-sm');
        $('#' + this.tableID + '_filter').addClass('form-group form-group-sm');
        
        // fix pagination
        $('.dataTables_paginate.paging_bootstrap').removeClass('pagination');
        $('.dataTables_paginate.paging_bootstrap ul').addClass('pagination pagination-sm dataTables-pagination');
        $('.dataTables_paginate.paging_bootstrap li:first a').html('&laquo;');
        $('.dataTables_paginate.paging_bootstrap li:last a').html('&raquo;');
    }
};
