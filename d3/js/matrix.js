var header;
var head=0;
window.onload = function () {

        d3.csv("data/formatted_similarity_calc.csv", function (error,data) {
           console.log("in");
			if (error)
				throw error;
            var label_col_full = Object.keys(data[0]);
			header = d3.keys(data[0]);
            var label_row = [];
            var rows = [];
            var row = [];
			var temp;
            for (var i = 0; i < data.length; i++) {
				temp=data[i][label_col_full[0]];
                label_row.push(temp);
                row = [];
				
                for (var j = 1; j < label_col_full.length; j++) {
					
					temp=parseFloat(data[i][label_col_full[j]]);
                    row.push(temp);
					
                }
                rows.push(row);
				
            }
            
            d3.select("svg").remove();
            d3.select("rowLabelg").remove();
            main(rows, label_col_full.slice(1), label_row);
            
        });
};

var mapsize = 2000;
var pixelsize = 20;
var cellsize = pixelsize-1;

d3.select('.tooltip').style('padding',' 10px')
.style('background',' white')
.style('border-radius',' 10px')
.style('box-shadow',' 4px 4px 10px rgba(0, 0, 0, 0.4)');

var main = function (corr, label_col, label_row) {

    var transition_time = 1500;
    var body = d3.select('body');
	body.select('g.legend').style('position','absolute')
	.style('height','25px')
	.style('width','400px').style('margin','auto').style('margin-left','100px')        
	.style('background','linear-gradient(to right,#c8f2b9,#db3db6)');
    var tooltip = body.select('div.tooltip');
    var svg = body.select('#chart').append('svg')
        .attr('width', mapsize*3-500)
        .attr('height', mapsize-1400).style('margin','auto').style('margin-top','-50px').style('margin-left','150px');;;

  
    var row = corr;
    var col = d3.transpose(corr);
	var total_len ;

    var indexify = function (mat) {
        var res = [];
		total_len = mat.length;
		console.log(total_len);
        for (var i = 0; i < mat.length; i++) {
            for (var j = 0; j < mat[0].length; j++) {
				if(isNaN(mat[i][j]))
					temp = 0;
				else
					temp=mat[i][j];
                res.push({
                    i: i,
                    j: j,
                    val: temp
			
                });
		
            }
	
        }
        return res;
    };

    var corr_data = indexify(corr);
    var order_col = d3.range(label_col.length + 1);
    var order_row = d3.range(label_row.length + 1);

    var color = d3.scale.linear()
        .domain([ 0, 1])
        .range(['#c8f2b9', '#db3db6']);

    var scale = d3.scale.linear()
        .domain([0, d3.min([50, d3.max([label_col.length, label_row.length, 4])])])
        .range([0, parseFloat(1) * 250]);

   

    var label_space = 50;

    var matrix = svg.append('g')
        .attr('class', 'matrix')
	.attr('height',mapsize-1400)
	.attr('width',mapsize*3-500)
        .attr('transform', 'translate(' + (label_space + 10) + ',' + (label_space + 10) + ')')
	.selectAll('rect.pixel').data(corr_data)
	.enter().append('rect')
        .attr('class', 'pixel')
        .attr('width', cellsize)
        .attr('height', cellsize)
	.attr('position','absolute')
	.attr('y',function(d){return d.i*pixelsize+ label_space-5})
	.attr('x',function(d){return d.j*pixelsize + label_space})
        .style('fill', function (d) {
            return color(d.val);
        })
        .on('mouseover', function (d) {
	       tooltip.style("opacity", 0.8)
	    .style('position', 'absolute')
            .style("left", (d3.event.pageX + 35) + 'px')
            .style("top", (d3.event.pageY + 30) + 'px')
            .html('File: '+ header[d.i+1] +"<br>" + "File: " +header[d.j+1] + "<br>" + "Value: " + d.val.toFixed(3));


		d3.select(this).style("opacity", 0.5);
        })
        .on('mouseout', function (d) {
            tooltip.style("opacity", 1e-6);
	    d3.select(this).style("opacity", 1);
        });
   

rowLabel = []
colLabel = []

for(var head=1; head<header.length;head++)
{
rowLabel.push(header[head])
colLabel.push(header[head]);
}

    console.log(rowLabel);
var rowLabels = svg.append("g")
      .selectAll(".rowLabelg")
      .data(rowLabel)
      .enter()
      .append("text")
      .attr("class","rowLabelg")
      .text(function (d) { return d; })
      .style('font-size','9px')
      .attr("x", 0)
      .attr("y", function (d, i) { return i * pixelsize; })
      .style("text-anchor", "end")
      .attr("transform", "translate(107,115)");
     

  var colLabels = svg.append("g")
      .selectAll(".colLabelg")
      .data(colLabel)
      .enter()
      .append("text")
      .attr("class","colLabelg")
      .text(function (d) { return d; })
      .style('font-size','9px')
      .attr("x", 0)
      .attr("y", function (d, i) { return i * pixelsize; })
      .style("text-anchor", "left")
      .attr("transform", "translate(120,100) rotate (-90)");



};
 
