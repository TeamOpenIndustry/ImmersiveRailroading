for mtl in $(find src/main/resources/assets/immersiverailroading/models/rolling_stock/ | grep "mtl$"); do
	echo $mtl
	dir=$(dirname $mtl)
	kdmap=$(cat $mtl | grep map_Kd | sed -e 's/map_Kd // ')
	for fpath in $(find $dir | grep png); do
		fname=$(basename $fpath)
		if [[ $kdmap != *"$fname"* ]]; then
			echo "$fname UNUSED"
			rm -rf $fpath
		fi
	done

	pngmap=$(find $dir | grep png)
	for fname in $(cat $mtl | grep map_Kd | sed -e 's/map_Kd // '); do
		if [[ $pngmap != *"$fname"* ]]; then
			echo "$fname MISSING"
		fi
	done
done
