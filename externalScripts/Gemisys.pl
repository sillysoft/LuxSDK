#! /usr/bin/perl -I. -w
#
# gemisys  (map builder for lux)
#
# Things to do:
#
# If you find a bug, or a size tuple that doesn't work, or you simply want
# to make suggestions, please use this address:  gemisys_support@ferkel.co.uk
# If you've hacked up the script in any way, please make sure that your bug
# is in the original version!  I won't fix your code for you (except on a 
# standard consultancy fee-for-services contract, of course :).
#
# Thanks!
# rip
# -- 
# Richard P. Williamson
# Porta Nigra Software 
# VxWorks, C, Perl, OS internals, web
# And embedded systems programmer
#
# Also available for documentation review and translation services
# (German, French and Russian source languages, German, English target)
#
# modification history
# -----------------------
# 02b,08jul04,rip .0 LuxMapGen 4.0 Beta revisions/cleanup
# 02a,08jun04,rip .0 fwv of MapGen plug-in
##

######### ######### ######### ######### ######### ######### ######### ##########
#         configure
######### ######### ######### ######### ######### ######### ######### ##########
my(@VERSION)= &getVersion();
my($VERSION) = join(":", @VERSION);

my($debug) = 0;
my($debugTrace) = "";

my($onepi) = 3.141592653589793;
my($twopi) = 2 * $onepi;
my($srand) = 0;

my($attempts) = 30;

# By adding widthxheight options here, they will automatically appear in
# Lux (and they might even work, too!)
my(@Choices) = (qw(450x360 600x400 680x480 700x500 800x600 900x700));

# set unbuffered io 
select((select(STDOUT), $| = 1)[0]);
select((select(STDERR), $| = 1)[0]);

print STDERR ("gemisys.pl called with args @ARGV\n");

my($sized);

# a map will be comprised of standard values for most of the configurables,
# based on the sized (600x400, etc) handed it, and the srand seed
foreach (@ARGV) {
  /^choices$/ && do {
    &announceChoices();
    exit(1);
  };
  s/^Gemisys_// && do {
    $sized = $_;
    next;
  };
  /^(\d+)x(\d+)$/ && do {
    $sized = $_;
    next;
  };
  srand($_);
}

# global data
my($masterid) = 0;
my($info) = "";
my(@gal);
my(%PTS) = ();

# galactic defaults
my($swid, $shei) = (800, 600);
my($whjCnt) = 100;
my($cmin, $cmax) = (4, 10);
my($pipes) = 12;
my($threads) = int($pipes/2);
my($neighborhood) = 1000;
my($RI) = (16); # internal radius (ie, centerpoint radius) smaller=larger
my($RO) = (38); # external radius (ie, center + petals) larger=larger
my($ROFF) = (60); # Offset radius (exclusion zone) larger=larger
my($bonusmult) = .66; # change this to adjust continent bonus values

if(defined($sized)) { 
  # 600x400 # 680x480 # 700x500 # 800x600 etc
  ($swid, $shei) = $sized =~ /(\d+)x(\d+)/;
}


######### ######### ######### ######### ######### ######### ######### ##########
#                   define session
######### ######### ######### ######### ######### ######### ######### ##########
# Assume that all preconfigured-based-on-sized has happened.

# need to ensure a minimum offset between continents.
# outside and inside radii of a junction and its petals
my($radIn)  = int((($shei + $swid) / 2) / $RI); #
my($radOut) = $radIn + $RO;
my($radOff) = $radOut + $ROFF;

my(@keep) = "";

# Do something intelligent
print &makeMap(0);

if($debug) { &debugTraceDump(); }
    
# and we're done.
exit 1;

######### ######### ######### ######### ######### ######### ######### ##########
#                             work task 
######### ######### ######### ######### ######### ######### ######### ##########
# announceChoices - yyy
#
# params - none
# frm - none
#
##
sub announceChoices { # yyy
  foreach (@Choices) { print("$_\n"); }
  return();
}

################################################################################
sub makeMap {

  my($a) = shift;
  if($a++ > 15) {
    return("Failed! Recursion too deep!  Try again!");
  }

  my($htm) = "";

  my($i);

  my($XMLl) = "";
  my($XML) = qq^
<luxboard>
  <version>1.1</version>
  <width>${swid}</width>
  <height>${shei}</height>
  <theme>space</theme>
  <title>Gemisys $sized</title>
  <author>rip</author>
  <email>rip\@ferkel.co.uk</email>
  <webpage>http://risk.ferkel.co.uk/cgi/gemisys.cgi</webpage>
#LINES#
#CONTINENTS#
</luxboard>  
^;

  my($cont) = 0;
  my(@bonus) = ();
  my($totalcc) = 0;

  my($infloop) = 0;
  my($maxTimes) = 5;
restartPlace:

  # Make sure we don't go round and round and round...
  if(!$maxTimes--) {
    &debugOut("MaxTimes exceeded, going with best effort");
    @gal = @keep;
    goto BeginWork;
  }
    
  &debugOut("(re)Starting Galactic Placement...");
  $masterid = 0;
  $infloop = 350;
  my($mult) = 60;

  for($i = 0; $i < $whjCnt; $i++) {  
    # we need this here so the offsetter can use them.
    my($x, $y) = &randomPoint();
    my($r) = ((rand($mult) - ($mult / 2)) / 100) + 1;
    my($th) = (rand($twopi * 1000))/1000; # rand(2*pi*1000) / 1000
    my($ok) = &ensureOffset($i, $x, $y, $r);

    while(!$ok) {
      if(!$infloop--) { # badness.  restart with new placement;
        # but first, store the preceding if scalar > previous failure
        goto restartPlace;
      }
      ($x, $y) = &randomPoint();
      $ok = &ensureOffset($i, $x, $y, $r);
    }

    # we are here, which means that we have enough offset.
    my(%T) = %{&getHash()};
    $gal[$i] = \%T;

    my($c) = int(rand(($cmax - $cmin) + 1)) + $cmin; # countries

    # id stamp these now.  This means that the termini reside in slots 
    # 0 - n.
    $gal[$i]{"id"} = $masterid++;
    $gal[$i]{"x"} = $x;
    $gal[$i]{"y"} = $y;
    $gal[$i]{"radii"} = $r;
    $gal[$i]{"theta"} = $th;
    $gal[$i]{"armylocation"} = "$x,$y";
    $gal[$i]{"c"} = $c;
    $gal[$i]{"continentname"} = "Cont_$i";

    # number of (at this point, mostly virtual) countries on the map
    $totalcc += (1 + $c);

    &debugOut("$i: r $r, t $th, c $c ($totalcc), cp $x,$y");
    if($i > scalar(@keep)) { 
      &debugOut("Better effort " . scalar(@keep) . " vs " . scalar(@gal));
      @keep = @gal; 
    }
  }
  &debugOut("Count $whjCnt requested, we aimed to please.");

BeginWork:
  $whjCnt = scalar(@gal);
  $masterid = $whjCnt;

  &debugOut("Wormhole generation... ($whjCnt junctions)");
  # @gal is the termini.
  my(@nn) = (); # only used if NN
    @nn = &nearestNeighbor(@gal);
    # @nn is a list of "0-3", "1-4", "1-3" etc pairs
    foreach (@nn) {
      my($ai, $aj) = split(/-/, $_);
      if((exists($gal[$ai]{"adjoining"})) && ($gal[$ai]{"adjoining"} ne "")) {
        $gal[$ai]{"adjoining"} .= ",$aj";
      } else {
        $gal[$ai]{"adjoining"} = "$aj";
      }
      if((exists($gal[$aj]{"adjoining"})) && ($gal[$aj]{"adjoining"} ne "")) {
        $gal[$aj]{"adjoining"} .= ",$ai";
      } else {
        $gal[$aj]{"adjoining"} = "$ai";
      }
    }

  &debugOut("Wormhole creation...");
  my(@curcol) = ("1.0/1.0/1.0");
  my($curcol) = 0;
  my(%L) = ();
  for($i = 0; $i < $whjCnt; $i++) {
    my(@adj) = split(/,/, $gal[$i]{"adjoining"});
    foreach (@adj) {
      if(exists($L{$i . $_})) { &debugOut("i is $i, _ is $_ *ignored*"); next; }
                $L{$_ . $i} = 1;
      &debugOut("i is $i, _ is $_");
      my($xi, $yi) = split(/,/, $gal[$i]{'armylocation'});
      my($xj, $yj) = split(/,/, $gal[$_]{'armylocation'});
      my($xi1, $xi2, $yi1, $yi2) = ($xi, $xi, $yi, $yi);
      my($xj1, $xj2, $yj1, $yj2) = ($xj, $xj, $yj, $yj);

      my($lpip) = int($pipes/3);

      $xi1 += $lpip; $yi1 += $lpip; $xi2 -= $lpip; $yi2 -= $lpip;
      $xj1 += $lpip; $yj1 += $lpip; $xj2 -= $lpip; $yj2 -= $lpip;

      # emit centerpoint-centerpoint lines
      $XMLl .= qq^
  <line>
    <position>$xi,$yi $xj,$yj</position>
    <width>$lpip</width>
    <color>$curcol[$curcol]</color>
  </line>
  <line>
    <position>$xi1,$yi1 $xj,$yj $xi2,$yi2</position>
    <width>$lpip</width>
    <color>$curcol[$curcol]</color>
  </line>
  <line>
    <position>$xj1,$yj1 $xi,$yi $xj2,$yj2</position>
    <width>$lpip</width>
    <color>$curcol[$curcol]</color>
  </line>
^;
      $curcol = ($curcol + 1) % scalar(@curcol);
    }
  }
  undef %L;

  &debugOut("Termini population...");
  my(@c);
  for($i = 0; $i < $whjCnt; $i++) {
    if(!exists($gal[$i]{"c"})) {
      &debugOut("Warn: $i c not exist");
      die("c not exist");
    } elsif (!defined($gal[$i]{"c"})) {
      &debugOut("Warn: $i c not defined");
      die("c not defined");
    }
    $bonus[$i] = 0;
    
    # &debugOut("Jnctn $i ($gal[$i]{'continentname'}): $gal[$i]{'x'},$gal[$i]{'y'} c $gal[$i]{'c'}");

    # gal[$i] color not in this rev

    $gal[$i]{"name"} = "Junction_$gal[$i]{id}";
    $gal[$i]{"xml"} = "";

    my($s) = 4; # all non-terminus polys start out as 4 sided

    # and the delta for the points on the junction polygon is
    my($delta) = $twopi / $gal[$i]{"c"}; # in radians

    my($lri) = int($radIn * $gal[$i]{"radii"});
    my($lro) = int($radOut * $gal[$i]{"radii"});
    my($lroff) = int($radOff * $gal[$i]{"radii"});

    # for each country adjoined around the junction...
    for($j = 0; $j < $gal[$i]{"c"}; $j++) {
      my(%C) = %{&getHash()};
      my($cid) = $masterid++;  
      $gal[$cid] = \%C;
      $gal[$cid]{"id"} = $cid;
      $gal[$cid]{"term"} = $i;
      $gal[$cid]{"xml"} = "";
 
      # adjoins its two neighboring countries, as well as the junction
      my($a1, $a2);
      if($j == 0) {
        $a1 = $gal[$i]{"c"} - 1 + $cid; $a2 = $cid + 1; 
      } elsif ($j == ($gal[$i]{"c"} - 1)) {
        $a1 = $cid - 1; $a2 = $cid - $gal[$i]{"c"} + 1;
      } else {
        $a1 = $cid - 1; $a2 = $cid + 1; 
      }
      # &debugOut("$a1 <-> $cid <-> $a2 ($gal[$i]{'c'}:$j)");
      $gal[$cid]{"adjoining"} = $gal[$i]{"id"} . ",$a1,$a2";

      # and the junction adjoins it.
      $gal[$i]{"adjoining"} .= ",$gal[$cid]{id}";

      $gal[$cid]{"name"} = "Country_$gal[$cid]{id}";
 
      my(@x, @y) = (); 

      # this side polygon country angles
      my($do) = ($delta * $j) + $gal[$i]{'theta'};
      my($di) = ($delta * (($j + 1) % $gal[$i]{'c'})) + $gal[$i]{'theta'};
      while($do > $twopi) { $do -= $twopi; }
      while($di > $twopi) { $di -= $twopi; }
      my($da) = ($do + $di) / 2;
      if($di < $do) { 
        $da = $di - ($di + ($twopi - $do)) / 2;
      }    

      # &debugOut("gal[$i] $gal[$i]{'id'} $gal[$i]{'x'} $gal[$i]{'y'} Country $gal[$cid]{'id'}");
      # &debugOut("do is $do  da is $da  di is $di"); 
      $x[0] = $gal[$i]{"x"} + int($lri * cos($do)); 
      $y[0] = $gal[$i]{"y"} + int($lri * sin($do)); 
      $x[1] = $gal[$i]{"x"} + int($lro * cos($do)); 
      $y[1] = $gal[$i]{"y"} + int($lro * sin($do)); 
      $x[2] = $gal[$i]{"x"} + int($lro * cos($di)); 
      $y[2] = $gal[$i]{"y"} + int($lro * sin($di)); 
      $x[3] = $gal[$i]{"x"} + int($lri * cos($di)); 
      $y[3] = $gal[$i]{"y"} + int($lri * sin($di)); 
      $x[4] = $gal[$i]{"x"} + int(($lro * 0.95) * cos($da));
      $y[4] = $gal[$i]{"y"} + int(($lro * 1.05) * sin($da));

      $gal[$cid]{"polygon"} .= "$x[0],$y[0] $x[1],$y[1] $x[4],$y[4] $x[2],$y[2] $x[3],$y[3]"; 
      $gal[$i]{"polygon"} .= "$x[0],$y[0] ";  

      my($xc, $yc) = split(/,/, &cpoint(split(/ /, $gal[$cid]{"polygon"})));
      $gal[$cid]{"armylocation"} = "$xc,$yc";
      $gal[$cid]{"haslink"} = 0;

      $gal[$gal[$cid]{"id"}] = $gal[$cid];

    }
    $gal[$i]{"polygon"} =~ s/\s*$//;
    $gal[$gal[$i]{"id"}] = $gal[$i];

  }
 
  # galactic neighbors
  for($i = $whjCnt; $i < $masterid; $i++) {
    $gal[$i]{"close_id"} = 0;
    $gal[$i]{"close_d"} = 999999;
    for($j = $whjCnt; $j < $masterid; $j++) {
      # this check ensures we don't link a country to itself
      if($i == $j) { next; }

      # this check ensures we don't link a terminus' petals to each other.
      if($gal[$i]{"term"} == $gal[$j]{"term"}) { next; }

      my(@i) = split(/\s/, $gal[$i]{"polygon"});
      my(@j) = split(/\s/, $gal[$j]{"polygon"});

      my($d) = &dist($i[2], $j[2]);

      if($d < $neighborhood) {
        if($d < $gal[$i]{"close_d"}) {
          $gal[$i]{"close_id"} = "$j";
          $gal[$i]{"close_d"} = $d;
        }
      }
    }
  }

  my(@sorted) = ();
  for($i = $whjCnt; $i < $masterid; $i++) {
    if(defined($sorted[$gal[$i]{"close_d"}])) {
      $sorted[$gal[$i]{"close_d"}] .= "," . $i;
    } else {
      $sorted[$gal[$i]{"close_d"}] = $i;
    }
  }
  shift(@sorted); # parasitic sorted[0] for those too far away from anything
  my(@nsorted) = ();
  for($i = 1; $i < scalar(@sorted); $i++) {
    if(!defined($sorted[$i])) { next; }
    @nsorted = (@nsorted, split(/,/, $sorted[$i]));
  }

  my(@termcount) = (split(//, ("0" x 10)));
  my($neutrals) = $masterid;
  my($s);
  for($s = 0; $s < scalar(@nsorted); $s++) {
    my($i) = $nsorted[$s];
    if($gal[$i]{"close_d"} == 0) {
      &debugOut("$i already linked.  Skipping");
      next;
    }
    my($d) = $gal[$i]{"close_d"};
    my($j) = $gal[$i]{"close_id"};
    &debugOut("$i close_id is $j");

    if($j == 0) {
      &debugOut("$i close_id is zero (ie, nothing in neighborhood), skipping");
      next; 
    }
    if($gal[$j]{"close_id"} == $i) {
      &debugOut("$i and $j are close_id to each other.");
      $gal[$j]{"close_d"} = 0;
    }
    &debugOut("$i $j: $d");

    my($clue) = $gal[$i]{"haslink"} . $gal[$j]{"haslink"};
    if($clue =~ /3/) { 
      &debugOut("haslink 3 (ij=$clue)");
      next;
    } 

    # are they facing?
    my($valid, @newpoly) = &makeNeutral($i, $j);
    if(!$valid) { 
      &debugOut("makeNeutral($i, $j) returned NO");
      next; 
    }

    $k = $masterid++; 
    $gal[$k] = &getHash();

    $bonus[$gal[$i]{"term"}] += $bonusmult; 
    $bonus[$gal[$j]{"term"}] += $bonusmult;

    $gal[$k]{"id"} = $k;
    if($k % 2) { $gal[$k]{"term"} = $gal[$i]{"term"}; }
    else { $gal[$k]{"term"} = $gal[$j]{"term"}; }
    $gal[$k]{"polygon"} = join(" ", @newpoly);
    $gal[$k]{"adjoining"} = "$i,$j";
    $gal[$k]{"name"} = "Neutral $k";
    $gal[$k]{"armylocation"} = &cpoint(split(/\s/, ($gal[$k]{"polygon"})));
    $gal[$i]{"adjoining"} .= ",$k";
    $gal[$j]{"adjoining"} .= ",$k";

    # connecting up laterally
    my(@pts) = split(/\s/, $gal[$k]{"polygon"});
    @pts = (@pts, $pts[0]);
    my($pt) = 0;
    my(@mge) = ();
    for($pt = 0; $pt < (scalar(@pts) - 1); $pt++) {
      my($lseg) = "$pts[$pt] $pts[($pt+1)]";
      if(exists($PTS{$lseg})) {
        $gal[$k]{"adjoining"} .= "," . $PTS{$lseg};
        $gal[$PTS{$lseg}]{"adjoining"} .= "," . $k;
        delete $PTS{$lseg};
        next;
      }
      $PTS{$lseg} = $k;
      $lseg = "$pts[($pt+1)] $pts[$pt]";
      if(exists($PTS{$lseg})) {
        $gal[$k]{"adjoining"} .= "," . $PTS{$lseg};
        $gal[$PTS{$lseg}]{"adjoining"} .= "," . $k;
        delete $PTS{$lseg};
        next;
      }
      $PTS{$lseg} = $k;
    }
  }  
  # end galactic neighbors

  for ($i = 0; $i < $whjCnt; $i++) {
    $gal[$i]{"xml"} = &BuildContXML($i);
    my($b) = int($bonus[$i]);
    $gal[$i]{"xml"} =~ s/BONUS_$i/$b/;
  }

  my($XMLc) = "";
  my(@xml) = ();
  for ($i = $whjCnt; $i < $masterid; $i++) {
    my($t) = $gal[$i]{"term"};
    if(!defined($xml[$t])) { $xml[$t] = ""; }
    $xml[$t] .= &BuildXML($i);
  }
  for ($i = 0; $i < $whjCnt; $i++) {
    $gal[$i]{"xml"} =~ s/#COUNTRY_XML#/$xml[$i]/;
    $XMLc .= $gal[$i]{"xml"} . "\n";
  }

  $XML =~ s/#CONTINENTS#/$XMLc/;
  $XML =~ s/#LINES#/$XMLl/;

  return($XML);
}

sub getHash { local(%N) = (); return(\%N); }
sub torad { return($_[0] / $twopi); }

###############################################################################
###############################################################################
# debug code - 
###############################################################################
###############################################################################
sub errorOut { &debugOut("cgi error:  $_[0]"); }
sub debugTraceDump {
  print STDERR $debugTrace;
}
sub debugGroupIn { 
  my(@d) = @_;
  my(@a) = caller(1);
  if(!defined($a[0])) {
    # top level
    @a = caller(0);
  }
  my($a) = "($a[0]) <font color=red>$a[1]</font>:$a[2] ($a[3])";
  $debugTrace .= "\n<dir><b>IN</b> " . $a . "<br>@d<br>\n";
  $a = $a[3]; $a =~ s/.*:://;
  push(@debugList, $a);
}
sub debugGroupOut { 
  my($a) = pop(@debugList); 
  if(defined($a)) { 
    $a = ": <font color=red>$a</font>"; 
  } else { $a = ""; }
  my($b) = shift; if(!defined($b)) { $b = ""; }
  $debugTrace .= "\n<br><b>OUT ($b)</b>" . $a . "</dir>\n"; 
}
sub debugOutDefangHtml { 
  my(@a) = @_;
  foreach(@a) {
    s/<script/&lt;DEFANG_script/gi;
    s/<form/&lt;DEFANG_form/gi;
    s/</&lt;/g;
    s/>/&gt;/g;
  }
  debugOut(@a);
}

sub debugOut { 
    while(scalar(@_) > 0) {
      my($a) = shift;
      if(!defined($a)) { 
        next; 
      } elsif ($a =~ /^defang/) {
        $a = shift;
        $a =~ s/\</\&lt\;/g;
        $a =~ s/\>/\&gt\;/g;
        if($a =~ /^warn/i) { $a = "<font color=red>" . $a . "</font>"; }
        $debugTrace .= "$a <br>\n";
      } else {
        if($a =~ /^warn/i) { $a = "<font color=red>" . $a . "</font>"; }
        $debugTrace .= "$a <br>\n"; 
      }
    } 
}

sub printEnv { 
  my($in) = shift;
  my(%in) = %$in;
  my($key, $val);  
  while (($key, $val) = each %in) { 
    &debugOut("$key = $val");
  } 
}
################################################################################
# getVersion - returns ('dda', 'dddddd', 'www', 'minor', 'what...')
#
# param: file name
##
sub getVersion {
  my($fn) = shift;
  if(!defined($fn) || $fn eq "") {
    $fn = $0;
  }
  my(@ver) = ("", "", "", "", "");
  open(FI, "$fn") or return (@ver);

  my($x);
  while(defined($x = <FI>)) {
    my($a); while(($a = chop $x) =~ /\s/) {} $x .= $a;
    if($x =~ /^#\s*(\d\d.),(\d\d...\d\d),(...?)\s+(\.\d+)\s+(.*)/) {
      @ver = ($1, $2, $3, $4, $5);
REDO:
      if(defined($x = <FI>)) {
        my($a); while(($a = chop $x) =~ /\s/) {} $x .= $a;
        if($x =~ /^#\s+(\.\d+)\s+(.*)/) {
          $ver[3] = $1; 
          $ver[4] .= ", " . $2;
          goto REDO;
        }
      }
      last;
    }
  }
  close FI;
  return(@ver);
}

sub acos { atan2( sqrt(1 - $_[0] * $_[0]), $_[0] ); }
sub asin { atan2($_[0], sqrt(1 - $_[0] * $_[0])); }

sub ptarray {
  my(@a) = @_;
  my(@b) = ();
  foreach (@a) {
    if($_ =~ /,/) {
      my($a, $b) = split(/,/, $_);
      push(@b, $a, $b);
    } else {
      push(@b, $_);
    }
  }
  return(@b);
}

sub dist {
  my(@tmp) = &ptarray(@_);
  if($tmp[0] =~ /,/) { @pts = split(/,/, $tmp[0]); } else {
    $pts[0] = shift(@tmp); $pts[1] = shift(@tmp);
  }
  if($tmp[0] =~ /,/) { @pts = (@pts, split(/,/, $tmp[0])); } else {
    $pts[2] = shift(@tmp); $pts[3] = shift(@tmp);
  }
  my($distx) = ($pts[0] - $pts[2]); 
  my($disty) = ($pts[1] - $pts[3]);
  my($dist) = int(sqrt(($distx * $distx) + ($disty * $disty)));
  return($dist);
}  

sub theta {
  my(@a) = &ptarray(@_);
  my($dist) = &dist(@a);
  my($tht) = (&acos(($a[2] - $a[0]) / $dist));
  if($a[2] < $a[0]) { $tht += $onepi; }
  # &debugOut("theta: @a => $dist at ($tht)");
  return($tht);
}

sub geogcpoint {
  my(@a) = @_;
  my($xb, $xs, $yb, $ys) = (0, $swid, 0, $shei);
  foreach (@a) {
    my($x, $y) = split(/,/, $_);
    if($x < $xs) { $xs = $x; } elsif ($x > $xb) { $xb = $x; }
    if($y < $ys) { $ys = $y; } elsif ($y > $yb) { $yb = $y; }
  }
  $xb = int(($xb - $xs) / 2); $xb += $xs;
  $yb = int(($yb - $ys) / 2); $yb += $ys;
  return("$xb,$yb");
}

sub cpoint {
  my(@a) = @_;
  my($xc, $yc) = (0,0);
  foreach (@a) {
    if(!/,/) {
      &debugOut("error in cpoint: $_ dnm ,");
      next;
    }
    my($x, $y) = split(/,/, $_);
    $xc+=$x; $yc+=$y;
  }
  $xc = int($xc / scalar(@a)); 
  $yc = int($yc / scalar(@a)); 
  return("$xc,$yc");
}

sub nearestNeighbor {
  my(@terms) = @_;
  my($i, $j);

  my(%L) = ();
  for($i = 0; $i < scalar(@terms); $i++) {
    my($ki, $kd) = (-1, 9999);
    for($j = 0; $j < scalar(@terms); $j++) {
      if($j == $i) { next; }
      if(exists($L{$j . "-" . $i})) { next; }
      my($dist) = &dist($terms[$i]{"x"}, $terms[$i]{"y"}, $terms[$j]{"x"}, $terms[$j]{"y"}); 
      if($dist < $kd) { $ki = $j; $kd = $dist; }
    }
    if($ki == -1) { 
      # already links to everything.  bizarre boundary conditions are us.
    } else {
      $L{$i . "-" . $ki} = $kd;
    }
  }

  return(sort keys %L);
}
   
sub greatCircle {
  my(@terms) = @_; 
  my($i, $j);

  my(@tmp) = ();
  my(@srtd) = ();
  for($i = 0; $i< scalar(@terms); $i++) {
    $tmp[$i] = $terms[$i]{"x"} . "," . $terms[$i]{"y"};
  }
  
  my($cp) = &geogcpoint(@tmp);
  my(@theta) = ();
  my(@dists) = ();
  for($i = 0; $i < scalar(@terms); $i++) {
    $theta[$i] = &theta(split(/,/, $cp), split(/,/, $tmp[$i]));
    $dists[$i] = &dist(split(/,/, $cp), $terms[$i]{"x"}, $terms[$i]{"y"});
    $srtd[$i] = $i;
  }

  for($i = 0; $i < (scalar(@theta) - 1); $i++) {
    for($j = $i+1; $j < (scalar(@theta)); $j++) {
      if(($theta[$i] == $theta[$j]) && ($dists[$i] > $dists[$j])) {
        ($theta[$i], $theta[$j]) = ($theta[$j], $theta[$i]);
        ($dists[$i], $dists[$j]) = ($dists[$j], $dists[$i]);
        ($srtd[$i], $srtd[$j]) = ($srtd[$j], $srtd[$i]);
      } elsif($theta[$i] > $theta[$j]) {  
        ($theta[$i], $theta[$j]) = ($theta[$j], $theta[$i]);
        ($dists[$i], $dists[$j]) = ($dists[$j], $dists[$i]);
        ($srtd[$i], $srtd[$j]) = ($srtd[$j], $srtd[$i]);
      }
    }
  }
  for($i = 0; $i < scalar(@theta); $i++) {
    $srtd[$i] = $terms[$srtd[$i]];
  }

  return(@srtd);
} 

sub randomPoint {
  my($x) = int(rand($swid - $radOff) + ($radOff / 2));
  if($x < $radOff)           { $x = $radOff; }
  if($x > ($swid - $radOff)) { $x = $swid - $radOff; }

  my($y) = int(rand($shei - $radOff) + ($radOff / 2));
  if($y < $radOff)           { $y = $radOff; }
  if($y > ($shei - $radOff)) { $y = $shei - $radOff; }

  # &debugOut("Random Point called.  Returning $x,$y");
  return($x, $y);
}

sub ensureOffset {
  my($count, $x, $y, $rj) = @_;

  my($i);
  for($i = 0; $i < $count; $i++) {
    my($ix, $iy) = ($gal[$i]{"x"}, $gal[$i]{"y"}); 

    my($dist) = &dist($x, $y, $ix, $iy);
    if($dist == 0) { return(0); }
      
    my($ri) = int($gal[$i]{"radii"} * $radOut) + 5;
    my($rj) = int($rj * $radOut) + 5;

    if($dist < ($ri + $rj)) {
      # &debugOut("too close: $dist apart, " . ($ri + $rj) . " = $ri + $rj");
      return(0);
    }
  }
  return(1);
}

sub getAngle2 {
  my($xa, $ya, $xb, $yb);
  my($radordeg) = 0;
  if(scalar(@_) >= 2)  {
    ($xa, $ya) = split(/,/, shift);
    ($xb, $yb) = split(/,/, shift);
    if(scalar(@_)) { 
      $radordeg = shift;
    } 
  } else {
    return("-1");
  }
  my($r) = &dist($xa, $ya, $xb, $yb);
  my($dx) = ($xb - $xa);
  my($dy) = ($yb - $ya);
  my($thetatan) = atan2($dy,$dx);
  my($thetadtan) = $thetatan * (360/$twopi);

  &debugOut("pta $xa,$ya ptb $xb,$yb dist $r");
  &debugOut("gA2 (atan) = $thetatan ($thetadtan)");

  if($radordeg) {
    return($thetatan * (360/$twopi));
  } else {
    return($thetatan);
  }
}

sub getAngle3 {
  my($pta, $ptb, $ptc) = @_;
  my($th0) = &getAngle2($pta, $ptb);
  my($th1) = &getAngle2($ptb, $ptc);
  my($theta) = abs($th1 - $th0);

  my($thetd) = $theta * (360/$twopi);
  # &debugOut("getAngle3 $pta $ptb $ptc = abs($th1 - $th0) = $theta ($thetd degrees)");

  return($thetd);
}

sub makeNeutral {
  my($i, $j) = @_;
  my($ia, $ib, $ic, $id, $ie) = split(/\s/, $gal[$i]{"polygon"});
  my($ja, $jb, $jc, $jd, $je) = split(/\s/, $gal[$j]{"polygon"});
  my($yes, $no) = (1, 0);
  my($n) = 0;

  my($clue) = "i" . $gal[$i]{"haslink"} . "j" . $gal[$j]{"haslink"};
  &debugOut("makeNeutral called with $i and $j ($clue)");
  if($clue =~ /3/) { 
    &debugOut("No: $clue haslink 3"); 
    return($no); 
  }

  my(@newpoly);
  $gal[$i]{"haslink"} = 3;
  $gal[$j]{"haslink"} = 3;
  push (@newpoly, $ib, $ic, $id, $jb, $jc, $jd); 

  # check for squareness.
  # we do this by checking segments 2 and 5 (the crossings), looking for places 
  # where they cross any other segment. So long as they don't, we are ok.
  # segment 2= $newpoly[2],$newpoly[3], need to check 0,1, 4,5, 5,0
  # segment 5= $newpoly[5],$newpoly[0], need to check 1,2, 2,3, 3,4
  my($x1,$y1) = split(/,/, $newpoly[2]);
  my($x2,$y2) = split(/,/, $newpoly[3]);
  if($x1 != $x2) {
    foreach $n ("0,1", "4,5", "5,0") {
      my($n0,$n1) = split(/,/, $n);
      my($u1,$v1) = split(/,/, $newpoly[$n0]);
      my($u2,$v2) = split(/,/, $newpoly[$n1]);
      if($u2 != $u1) {
        my($b1) = ($y2 - $y1)/($x2 - $x1);
        my($b2) = ($v2 - $v1)/($u2 - $u1);
        my($a1) = $y1 - ($b1 * $x1);
        my($a2) = $v1 - ($b2 * $u1);
        if($b1 != $b2) {
          my($xi) = -1 * (($a1 - $a2) / ($b1 - $b2));
          my($yi) = $a1 + ($b1 * $xi);
          if    ((($x1-$xi) * ($xi-$x2) >= 0) 
              && (($u1-$xi) * ($xi-$u2) >= 0)
              && (($y1-$yi) * ($yi-$y2) >= 0)
              && (($v1-$yi) * ($yi-$v2) >= 0)) {
            # lines cross
            return($no);
          }
        }
      }
    }
  }  
  # segment 5= $newpoly[5],$newpoly[0], need to check 1,2, 2,3, 3,4
  ($x1,$y1) = split(/,/, $newpoly[5]);
  ($x2,$y2) = split(/,/, $newpoly[0]);
  if($x1 != $x2) {
    foreach $n ("1,2", "2,3", "3,4") {
      my($n0,$n1) = split(/,/, $n);
      my($u1,$v1) = split(/,/, $newpoly[$n0]);
      my($u2,$v2) = split(/,/, $newpoly[$n1]);
      if($u2 != $u1) {
        my($b1) = ($y2 - $y1)/($x2 - $x1);
        my($b2) = ($v2 - $v1)/($u2 - $u1);
        my($a1) = $y1 - ($b1 * $x1);
        my($a2) = $v1 - ($b2 * $u1);
        if($b1 != $b2) {
          my($xi) = -1 * (($a1 - $a2) / ($b1 - $b2));
          my($yi) = $a1 + ($b1 * $xi);
          if    ((($x1-$xi) * ($xi-$x2) >= 0) 
              && (($u1-$xi) * ($xi-$u2) >= 0)
              && (($y1-$yi) * ($yi-$y2) >= 0)
              && (($v1-$yi) * ($yi-$v2) >= 0)) {
            # lines cross
            return($no);
          }
        }
      }
    }
  }  

  return($yes, @newpoly);
}

sub BuildXML {
  my($h) = shift;
 
  my($xml) = qq^    <country>
      <id>$gal[$h]{'id'}</id>
      <polygon>$gal[$h]{'polygon'}</polygon>
      <adjoining>$gal[$h]{'adjoining'}</adjoining>
      <name>$gal[$h]{'name'}</name>
      <armylocation>$gal[$h]{'armylocation'}</armylocation>
    </country>
^;

  return($xml); 
} 

sub BuildContXML {
  my($h) = shift;
 
  my($cxml) = &BuildXML($h);
 
  my($xml) = qq^  <continent>
    <continentname>$gal[$h]{"continentname"}</continentname>
    <bonus>BONUS_$h</bonus>
    $cxml
#COUNTRY_XML#
  </continent>
^;
  return($xml);
}

