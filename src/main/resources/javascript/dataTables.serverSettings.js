var dataTablesServerSettings = {
    init: function($identifier) {
        $identifier.dataTable({
            "sDom": "<'row'<'col-md-6'l><'col-md-6 text-right'f>r>t<'row'<'col-md-6 text-muted'i><'col-md-6 text-right'p>>",
            "iDisplayLength": 10,
            "sPaginationType": "bootstrap",
            "aaSorting": [] //this option disable sort by default, the user steal can use column names to sort the table
        });

        this.bootstrap3LookAndFeel();
    },
    bootstrap3LookAndFeel: function() {
        $('select[name=sitesTable_length]').addClass('form-control');
        $('#sitesTable_filter input').addClass('form-control');
        $('#sitesTable_length').addClass('form-group-sm');
        $('#sitesTable_filter').addClass('form-group-sm');
        $('.dataTables_paginate.paging_bootstrap').removeClass('pagination');
        $('.dataTables_paginate.paging_bootstrap ul').addClass('pagination pagination-sm dataTables-pagination');
        $('.dataTables_paginate.paging_bootstrap li:first a').html('&laquo;');
        $('.dataTables_paginate.paging_bootstrap li:last a').html('&raquo;');
    }
};
