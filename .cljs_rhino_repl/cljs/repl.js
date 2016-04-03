// Compiled by ClojureScript 1.8.34 {}
goog.provide('cljs.repl');
goog.require('cljs.core');
cljs.repl.print_doc = (function cljs$repl$print_doc(m){
cljs.core.println.call(null,"-------------------------");

cljs.core.println.call(null,[cljs.core.str((function (){var temp__4657__auto__ = new cljs.core.Keyword(null,"ns","ns",441598760).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(temp__4657__auto__)){
var ns = temp__4657__auto__;
return [cljs.core.str(ns),cljs.core.str("/")].join('');
} else {
return null;
}
})()),cljs.core.str(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join(''));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Protocol");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m))){
var seq__31066_31080 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m));
var chunk__31067_31081 = null;
var count__31068_31082 = (0);
var i__31069_31083 = (0);
while(true){
if((i__31069_31083 < count__31068_31082)){
var f_31084 = cljs.core._nth.call(null,chunk__31067_31081,i__31069_31083);
cljs.core.println.call(null,"  ",f_31084);

var G__31085 = seq__31066_31080;
var G__31086 = chunk__31067_31081;
var G__31087 = count__31068_31082;
var G__31088 = (i__31069_31083 + (1));
seq__31066_31080 = G__31085;
chunk__31067_31081 = G__31086;
count__31068_31082 = G__31087;
i__31069_31083 = G__31088;
continue;
} else {
var temp__4657__auto___31089 = cljs.core.seq.call(null,seq__31066_31080);
if(temp__4657__auto___31089){
var seq__31066_31090__$1 = temp__4657__auto___31089;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__31066_31090__$1)){
var c__30708__auto___31091 = cljs.core.chunk_first.call(null,seq__31066_31090__$1);
var G__31092 = cljs.core.chunk_rest.call(null,seq__31066_31090__$1);
var G__31093 = c__30708__auto___31091;
var G__31094 = cljs.core.count.call(null,c__30708__auto___31091);
var G__31095 = (0);
seq__31066_31080 = G__31092;
chunk__31067_31081 = G__31093;
count__31068_31082 = G__31094;
i__31069_31083 = G__31095;
continue;
} else {
var f_31096 = cljs.core.first.call(null,seq__31066_31090__$1);
cljs.core.println.call(null,"  ",f_31096);

var G__31097 = cljs.core.next.call(null,seq__31066_31090__$1);
var G__31098 = null;
var G__31099 = (0);
var G__31100 = (0);
seq__31066_31080 = G__31097;
chunk__31067_31081 = G__31098;
count__31068_31082 = G__31099;
i__31069_31083 = G__31100;
continue;
}
} else {
}
}
break;
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m))){
var arglists_31101 = new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_((function (){var or__29897__auto__ = new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__29897__auto__)){
return or__29897__auto__;
} else {
return new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m);
}
})())){
cljs.core.prn.call(null,arglists_31101);
} else {
cljs.core.prn.call(null,((cljs.core._EQ_.call(null,new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.first.call(null,arglists_31101)))?cljs.core.second.call(null,arglists_31101):arglists_31101));
}
} else {
}
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"special-form","special-form",-1326536374).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Special Form");

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.contains_QMARK_.call(null,m,new cljs.core.Keyword(null,"url","url",276297046))){
if(cljs.core.truth_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))){
return cljs.core.println.call(null,[cljs.core.str("\n  Please see http://clojure.org/"),cljs.core.str(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))].join(''));
} else {
return null;
}
} else {
return cljs.core.println.call(null,[cljs.core.str("\n  Please see http://clojure.org/special_forms#"),cljs.core.str(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join(''));
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Macro");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"REPL Special Function");
} else {
}

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
var seq__31070 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"methods","methods",453930866).cljs$core$IFn$_invoke$arity$1(m));
var chunk__31071 = null;
var count__31072 = (0);
var i__31073 = (0);
while(true){
if((i__31073 < count__31072)){
var vec__31074 = cljs.core._nth.call(null,chunk__31071,i__31073);
var name = cljs.core.nth.call(null,vec__31074,(0),null);
var map__31075 = cljs.core.nth.call(null,vec__31074,(1),null);
var map__31075__$1 = ((((!((map__31075 == null)))?((((map__31075.cljs$lang$protocol_mask$partition0$ & (64))) || (map__31075.cljs$core$ISeq$))?true:false):false))?cljs.core.apply.call(null,cljs.core.hash_map,map__31075):map__31075);
var doc = cljs.core.get.call(null,map__31075__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists = cljs.core.get.call(null,map__31075__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name);

cljs.core.println.call(null," ",arglists);

if(cljs.core.truth_(doc)){
cljs.core.println.call(null," ",doc);
} else {
}

var G__31102 = seq__31070;
var G__31103 = chunk__31071;
var G__31104 = count__31072;
var G__31105 = (i__31073 + (1));
seq__31070 = G__31102;
chunk__31071 = G__31103;
count__31072 = G__31104;
i__31073 = G__31105;
continue;
} else {
var temp__4657__auto__ = cljs.core.seq.call(null,seq__31070);
if(temp__4657__auto__){
var seq__31070__$1 = temp__4657__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__31070__$1)){
var c__30708__auto__ = cljs.core.chunk_first.call(null,seq__31070__$1);
var G__31106 = cljs.core.chunk_rest.call(null,seq__31070__$1);
var G__31107 = c__30708__auto__;
var G__31108 = cljs.core.count.call(null,c__30708__auto__);
var G__31109 = (0);
seq__31070 = G__31106;
chunk__31071 = G__31107;
count__31072 = G__31108;
i__31073 = G__31109;
continue;
} else {
var vec__31077 = cljs.core.first.call(null,seq__31070__$1);
var name = cljs.core.nth.call(null,vec__31077,(0),null);
var map__31078 = cljs.core.nth.call(null,vec__31077,(1),null);
var map__31078__$1 = ((((!((map__31078 == null)))?((((map__31078.cljs$lang$protocol_mask$partition0$ & (64))) || (map__31078.cljs$core$ISeq$))?true:false):false))?cljs.core.apply.call(null,cljs.core.hash_map,map__31078):map__31078);
var doc = cljs.core.get.call(null,map__31078__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists = cljs.core.get.call(null,map__31078__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name);

cljs.core.println.call(null," ",arglists);

if(cljs.core.truth_(doc)){
cljs.core.println.call(null," ",doc);
} else {
}

var G__31110 = cljs.core.next.call(null,seq__31070__$1);
var G__31111 = null;
var G__31112 = (0);
var G__31113 = (0);
seq__31070 = G__31110;
chunk__31071 = G__31111;
count__31072 = G__31112;
i__31073 = G__31113;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return null;
}
}
});

//# sourceMappingURL=repl.js.map