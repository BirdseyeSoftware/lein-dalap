{
 [["path-to-file" "output-file"]]
 [(when (has-meta? :cljs-browser)) (transform drop-form)
  #".*Exception" 'js/Error]
 ["build-id" ["path-to-file" "output-file"]]
 []
 }
