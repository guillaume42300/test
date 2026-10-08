package com.observateurbasket

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var teamA:EditText; private lateinit var teamB:EditText
    private lateinit var ref1:EditText; private lateinit var ref2:EditText
    private lateinit var period:Spinner; private lateinit var type:Spinner
    private lateinit var note:EditText; private lateinit var court:CourtView
    private lateinit var general:EditText; private lateinit var pos1:EditText; private lateinit var work1:EditText
    private lateinit var pos2:EditText; private lateinit var work2:EditText; private lateinit var bilanPeriod:Spinner
    private lateinit var output:TextView
    private var dictTarget:EditText?=null
    private val prefs by lazy { getSharedPreferences("observateur_basket", MODE_PRIVATE) }
    private val situations = JSONArray()
    private val bilans = JSONArray()

    override fun onCreate(b:Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        teamA=findViewById(R.id.teamA); teamB=findViewById(R.id.teamB); ref1=findViewById(R.id.ref1); ref2=findViewById(R.id.ref2)
        period=findViewById(R.id.period); type=findViewById(R.id.type); note=findViewById(R.id.note); court=findViewById(R.id.court)
        general=findViewById(R.id.general); pos1=findViewById(R.id.pos1); work1=findViewById(R.id.work1); pos2=findViewById(R.id.pos2); work2=findViewById(R.id.work2)
        bilanPeriod=findViewById(R.id.bilanPeriod); output=findViewById(R.id.output)
        setupSpinners(); loadState()
        findViewById<Button>(R.id.dictate).setOnClickListener { dictTarget=note; dictate() }
        findViewById<Button>(R.id.dictateGeneral).setOnClickListener { dictTarget=general; dictate() }
        findViewById<Button>(R.id.saveSituation).setOnClickListener { saveSituation() }
        findViewById<Button>(R.id.addA).setOnClickListener { court.add("A1") }
        findViewById<Button>(R.id.addB).setOnClickListener { court.add("B1") }
        findViewById<Button>(R.id.addR).setOnClickListener { court.add("R1") }
        findViewById<Button>(R.id.clearCourt).setOnClickListener { court.clear() }
        findViewById<Button>(R.id.saveBilan).setOnClickListener { saveBilan() }
        findViewById<Button>(R.id.report).setOnClickListener { output.text=buildReport() }
    }
    private fun setupSpinners(){
        val periods=listOf("Q1","MI-TEMPS","Q3","FIN DE MATCH")
        period.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,periods)
        bilanPeriod.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,periods)
        val types=listOf("Décision / sifflet","Positionnement","Mécanique","Communication","Gestion du match","Point positif")
        type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,types)
    }
    private fun dictate(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),42); return
        }
        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toLanguageTag())
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Dictez votre observation")
        startActivityForResult(i,43)
    }
    override fun onActivityResult(r:Int,c:Int,d:Intent?){ super.onActivityResult(r,c,d); if(r==43 && c==RESULT_OK){ val s=d?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull(); if(!s.isNullOrBlank()) dictTarget?.append((if(dictTarget?.text.isNullOrBlank()) "" else " ")+s) } }
    private fun saveSituation(){
        if(note.text.toString().trim().isEmpty()){ toast("Décris la situation avant d'enregistrer."); return }
        val o=JSONObject().apply{
            put("period",period.selectedItem.toString()); put("type",type.selectedItem.toString()); put("note",note.text.toString().trim())
            put("court",JSONArray(court.markers.map{ JSONObject().put("label",it.label).put("x",it.x).put("y",it.y) }))
        }
        situations.put(o); persist(); note.text.clear(); court.clear(); toast("Situation enregistrée.")
    }
    private fun lines(s:String)=s.lines().map{it.trim()}.filter{it.isNotEmpty()}
    private fun saveBilan(){
        val fields=listOf(pos1 to "Arbitre 1 — positifs",work1 to "Arbitre 1 — travail",pos2 to "Arbitre 2 — positifs",work2 to "Arbitre 2 — travail")
        if(fields.any{ lines(it.first.text.toString()).size !in 2..3 }){ toast("Chaque arbitre doit avoir 2 à 3 points positifs et 2 à 3 pistes de travail."); return }
        bilans.put(JSONObject().apply{
            put("period",bilanPeriod.selectedItem.toString()); put("general",general.text.toString().trim())
            put("pos1",JSONArray(lines(pos1.text.toString()))); put("work1",JSONArray(lines(work1.text.toString())))
            put("pos2",JSONArray(lines(pos2.text.toString()))); put("work2",JSONArray(lines(work2.text.toString())))
        }); persist(); toast("Bilan enregistré.")
    }
    private fun persist(){
        prefs.edit().putString("teamA",teamA.text.toString()).putString("teamB",teamB.text.toString()).putString("ref1",ref1.text.toString()).putString("ref2",ref2.text.toString())
            .putString("situations",situations.toString()).putString("bilans",bilans.toString()).apply()
    }
    private fun loadState(){
        teamA.setText(prefs.getString("teamA","")); teamB.setText(prefs.getString("teamB","")); ref1.setText(prefs.getString("ref1","")); ref2.setText(prefs.getString("ref2",""))
        try{ val s=JSONArray(prefs.getString("situations","[]")); for(i in 0 until s.length()) situations.put(s.get(i)) }catch(_:Exception){}
        try{ val b=JSONArray(prefs.getString("bilans","[]")); for(i in 0 until b.length()) bilans.put(b.get(i)) }catch(_:Exception){}
    }
    private fun buildReport():String{
        val sb=StringBuilder()
        sb.append("OBSERVATION BASKETBALL\n\n").append(teamA.text).append(" — ").append(teamB.text).append("\n")
            .append("Arbitres : ").append(ref1.text).append(" / ").append(ref2.text).append("\n\n")
        sb.append("SYNTHÈSE DES BILANS\n")
        for(i in 0 until bilans.length()){
            val b=bilans.getJSONObject(i); sb.append("\n").append(b.getString("period")).append(" : ").append(b.getString("general")).append("\n")
        }
        sb.append("\nPOINTS POSITIFS / PISTES DE TRAVAIL\n")
        if(bilans.length()>0){
            val b=bilans.getJSONObject(bilans.length()-1)
            sb.append("\n").append(ref1.text).append(" — positifs : ").append(b.getJSONArray("pos1").join(", ")).append("\n")
            sb.append("Travail : ").append(b.getJSONArray("work1").join(", ")).append("\n")
            sb.append("\n").append(ref2.text).append(" — positifs : ").append(b.getJSONArray("pos2").join(", ")).append("\n")
            sb.append("Travail : ").append(b.getJSONArray("work2").join(", ")).append("\n")
        }
        sb.append("\nSITUATIONS OBSERVÉES\n")
        for(i in 0 until situations.length()){
            val s=situations.getJSONObject(i); sb.append("• ").append(s.getString("period")).append(" — ").append(s.getString("type")).append(" : ").append(s.getString("note")).append("\n")
        }
        return sb.toString()
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
